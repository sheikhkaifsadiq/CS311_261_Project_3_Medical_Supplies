package edu.psu.cs311.medicalsupplies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class BenchmarkRunner {

    private static final int WARMUP_TRIALS = 2;
    private static final int TRIALS = 5;
    private static final long SEED = 311261L;
    private static final List<Integer> PACKAGE_COUNTS = List.of(10, 20, 30, 40, 50);
    private static final List<Integer> CAPACITIES = List.of(20, 40, 60, 80, 100, 120);

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        run(Path.of("results"));
    }

    public static void run(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        List<BenchmarkRow> rows = new ArrayList<>();

        for (int packageCount : PACKAGE_COUNTS) {
            rows.addAll(measure("package-count", packageCount,
                    problem(packageCount, 100, SEED + packageCount)));
        }
        for (int capacity : CAPACITIES) {
            rows.addAll(measure("capacity", capacity,
                    problem(35, capacity, SEED + capacity * 17L)));
        }

        writeCsv(outputDirectory.resolve("benchmark-results.csv"), rows);
        writeEnvironment(outputDirectory.resolve("benchmark-environment.txt"));
        writeChart(outputDirectory.resolve("runtime-vs-package-count.svg"), rows,
                "package-count", "Runtime as package count increases", "Package count");
        writeChart(outputDirectory.resolve("runtime-vs-capacity.svg"), rows,
                "capacity", "Runtime as vehicle capacity increases", "Vehicle capacity (kg)");
        System.out.printf("Saved %d benchmark measurements and two charts to %s.%n",
                rows.size(), outputDirectory.toAbsolutePath());
    }

    private static List<BenchmarkRow> measure(
            String experiment, int size, ProblemInstance problem) {
        List<Long> greedyTimes = new ArrayList<>();
        List<Long> dynamicTimes = new ArrayList<>();
        List<Long> branchTimes = new ArrayList<>();
        AlgorithmResult greedy = null;
        AlgorithmResult dynamic = null;
        AlgorithmResult branch = null;

        for (int trial = 0; trial < WARMUP_TRIALS; trial++) {
            KnapsackAlgorithms.solveGreedy(problem);
            KnapsackAlgorithms.solveDynamicProgramming(problem);
            KnapsackAlgorithms.solveBranchAndBound(problem);
        }

        for (int trial = 0; trial < TRIALS; trial++) {
            greedy = KnapsackAlgorithms.solveGreedy(problem);
            dynamic = KnapsackAlgorithms.solveDynamicProgramming(problem);
            branch = KnapsackAlgorithms.solveBranchAndBound(problem);
            if (dynamic.totalImportance() != branch.totalImportance()) {
                throw new IllegalStateException("Exact methods disagreed during " + experiment + " benchmark.");
            }
            greedyTimes.add(greedy.runtimeNanos());
            dynamicTimes.add(dynamic.runtimeNanos());
            branchTimes.add(branch.runtimeNanos());
        }

        return List.of(
                row(experiment, size, problem, greedy, greedyTimes),
                row(experiment, size, problem, dynamic, dynamicTimes),
                row(experiment, size, problem, branch, branchTimes));
    }

    private static BenchmarkRow row(
            String experiment,
            int size,
            ProblemInstance problem,
            AlgorithmResult result,
            List<Long> timings) {
        return new BenchmarkRow(experiment, size, problem.packages().size(), problem.capacity(),
                result.algorithm(), median(timings), result.totalWeight(), result.totalImportance());
    }

    private static ProblemInstance problem(int packageCount, int capacity, long seed) {
        Random random = new Random(seed);
        List<SupplyPackage> packages = new ArrayList<>();
        for (int index = 0; index < packageCount; index++) {
            packages.add(new SupplyPackage(
                    "Package " + (index + 1), random.nextInt(18) + 1, random.nextInt(45) + 1));
        }
        return new ProblemInstance(packages, capacity);
    }

    private static long median(List<Long> timings) {
        List<Long> sorted = new ArrayList<>(timings);
        sorted.sort(Comparator.naturalOrder());
        return sorted.get(sorted.size() / 2);
    }

    private static void writeCsv(Path file, List<BenchmarkRow> rows) throws IOException {
        StringBuilder csv = new StringBuilder(
                "experiment,input_size,package_count,capacity,algorithm,median_runtime_ns,total_weight,total_importance\n");
        for (BenchmarkRow row : rows) {
            csv.append(row.experiment()).append(',')
                    .append(row.inputSize()).append(',')
                    .append(row.packageCount()).append(',')
                    .append(row.capacity()).append(',')
                    .append(row.algorithm()).append(',')
                    .append(row.medianRuntimeNanos()).append(',')
                    .append(row.totalWeight()).append(',')
                    .append(row.totalImportance()).append('\n');
        }
        Files.writeString(file, csv);
    }

    private static void writeEnvironment(Path file) throws IOException {
        String environment = "Benchmark date: " + java.time.LocalDate.now() + System.lineSeparator()
                + "Java version: " + System.getProperty("java.version") + System.lineSeparator()
                + "Operating system: " + System.getProperty("os.name") + " "
                + System.getProperty("os.arch") + System.lineSeparator()
                + "Trials per point: " + TRIALS + System.lineSeparator()
                + "Warm-up runs per point: " + WARMUP_TRIALS + System.lineSeparator()
                + "Random seed: " + SEED + System.lineSeparator()
                + "Package-count experiment: " + PACKAGE_COUNTS + " packages, capacity 100 kg" + System.lineSeparator()
                + "Capacity experiment: " + CAPACITIES + " kg, 35 packages" + System.lineSeparator();
        Files.writeString(file, environment);
    }

    private static void writeChart(
            Path file, List<BenchmarkRow> rows, String experiment,
            String title, String xAxisLabel) throws IOException {
        List<BenchmarkRow> chartRows = rows.stream()
                .filter(row -> row.experiment().equals(experiment))
                .toList();
        List<Integer> sizes = chartRows.stream().map(BenchmarkRow::inputSize).distinct().sorted().toList();
        List<String> algorithms = List.of(
                "Greedy Ratio Heuristic", "Dynamic Programming", "Branch and Bound");
        int width = 900;
        int height = 540;
        int left = 90;
        int right = 210;
        int top = 70;
        int bottom = 90;
        int plotWidth = width - left - right;
        int plotHeight = height - top - bottom;

        double minLog = Double.POSITIVE_INFINITY;
        double maxLog = Double.NEGATIVE_INFINITY;
        for (BenchmarkRow row : chartRows) {
            double value = Math.log10(Math.max(1.0, row.medianRuntimeNanos()) / 1_000_000.0);
            minLog = Math.min(minLog, value);
            maxLog = Math.max(maxLog, value);
        }
        if (maxLog - minLog < 0.5) {
            minLog -= 0.25;
            maxLog += 0.25;
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">\n")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n")
                .append("<text x=\"").append(width / 2).append("\" y=\"34\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"22\" font-weight=\"bold\">")
                .append(title).append("</text>\n");

        for (int tick = 0; tick <= 4; tick++) {
            double fraction = tick / 4.0;
            int y = top + plotHeight - (int) (fraction * plotHeight);
            double logValue = minLog + fraction * (maxLog - minLog);
            svg.append("<line x1=\"").append(left).append("\" y1=\"").append(y)
                    .append("\" x2=\"").append(left + plotWidth).append("\" y2=\"").append(y)
                    .append("\" stroke=\"#dddddd\"/>\n")
                    .append("<text x=\"").append(left - 10).append("\" y=\"").append(y + 5)
                    .append("\" text-anchor=\"end\" font-family=\"Arial\" font-size=\"12\">")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", Math.pow(10, logValue)))
                    .append("</text>\n");
        }
        svg.append("<line x1=\"").append(left).append("\" y1=\"").append(top + plotHeight)
                .append("\" x2=\"").append(left + plotWidth).append("\" y2=\"").append(top + plotHeight)
                .append("\" stroke=\"#333333\"/>\n")
                .append("<line x1=\"").append(left).append("\" y1=\"").append(top)
                .append("\" x2=\"").append(left).append("\" y2=\"").append(top + plotHeight)
                .append("\" stroke=\"#333333\"/>\n");

        for (int index = 0; index < sizes.size(); index++) {
            int x = xPosition(index, sizes.size(), left, plotWidth);
            svg.append("<line x1=\"").append(x).append("\" y1=\"").append(top + plotHeight)
                    .append("\" x2=\"").append(x).append("\" y2=\"").append(top + plotHeight + 5)
                    .append("\" stroke=\"#333333\"/>\n")
                    .append("<text x=\"").append(x).append("\" y=\"").append(top + plotHeight + 24)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"12\">")
                    .append(sizes.get(index)).append("</text>\n");
        }
        svg.append("<text x=\"").append(left + plotWidth / 2).append("\" y=\"").append(height - 28)
                .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\">")
                .append(xAxisLabel).append("</text>\n")
                .append("<text transform=\"translate(24 ").append(top + plotHeight / 2)
                .append(") rotate(-90)\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\">Median runtime (ms, log scale)</text>\n");

        String[] colors = {"#2d6cdf", "#e27a22", "#388e3c"};
        for (int algorithmIndex = 0; algorithmIndex < algorithms.size(); algorithmIndex++) {
            String algorithm = algorithms.get(algorithmIndex);
            List<BenchmarkRow> series = chartRows.stream()
                    .filter(row -> row.algorithm().equals(algorithm))
                    .sorted(Comparator.comparingInt(BenchmarkRow::inputSize))
                    .toList();
            StringBuilder points = new StringBuilder();
            for (int pointIndex = 0; pointIndex < series.size(); pointIndex++) {
                BenchmarkRow row = series.get(pointIndex);
                int x = xPosition(pointIndex, series.size(), left, plotWidth);
                double logValue = Math.log10(Math.max(1.0, row.medianRuntimeNanos()) / 1_000_000.0);
                int y = top + plotHeight - (int) ((logValue - minLog) / (maxLog - minLog) * plotHeight);
                points.append(x).append(',').append(y).append(' ');
                svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                        .append("\" r=\"4\" fill=\"").append(colors[algorithmIndex]).append("\"/>\n");
            }
            svg.append("<polyline fill=\"none\" stroke=\"").append(colors[algorithmIndex])
                    .append("\" stroke-width=\"2.5\" points=\"").append(points).append("\"/>\n")
                    .append("<line x1=\"").append(left + plotWidth + 24).append("\" y1=\"")
                    .append(top + 20 + algorithmIndex * 32).append("\" x2=\"")
                    .append(left + plotWidth + 48).append("\" y2=\"")
                    .append(top + 20 + algorithmIndex * 32).append("\" stroke=\"")
                    .append(colors[algorithmIndex]).append("\" stroke-width=\"3\"/>\n")
                    .append("<text x=\"").append(left + plotWidth + 56).append("\" y=\"")
                    .append(top + 25 + algorithmIndex * 32)
                    .append("\" font-family=\"Arial\" font-size=\"13\">")
                    .append(algorithm).append("</text>\n");
        }
        svg.append("<text x=\"").append(left + plotWidth + 20).append("\" y=\"")
                .append(top + 132).append("\" font-family=\"Arial\" font-size=\"12\" fill=\"#555\">Five-run median</text>\n")
                .append("</svg>\n");
        Files.writeString(file, svg);
    }

    private static int xPosition(int pointIndex, int pointCount, int left, int plotWidth) {
        return pointCount == 1 ? left + plotWidth / 2
                : left + pointIndex * plotWidth / (pointCount - 1);
    }

    private record BenchmarkRow(
            String experiment,
            int inputSize,
            int packageCount,
            int capacity,
            String algorithm,
            long medianRuntimeNanos,
            long totalWeight,
            long totalImportance) {
    }
}
