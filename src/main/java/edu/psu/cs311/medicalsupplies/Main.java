package edu.psu.cs311.medicalsupplies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--benchmark")) {
                BenchmarkRunner.run(Path.of("results"));
                return;
            }
            if (args.length == 1 && args[0].equals("--validate")) {
                ExhaustiveValidator.run(150, 15, 311261L);
                return;
            }
            if (args.length > 1) {
                throw new IllegalArgumentException("Usage: java -jar medical-supply-allocation.jar [input.csv | --benchmark | --validate]");
            }

            Path inputFile = args.length == 0 ? Path.of("data", "sample.csv") : Path.of(args[0]);
            ProblemInstance problem = InputFileReader.read(inputFile);
            printResults(problem, List.of(
                    KnapsackAlgorithms.solveGreedy(problem),
                    KnapsackAlgorithms.solveDynamicProgramming(problem),
                    KnapsackAlgorithms.solveBranchAndBound(problem)));
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void printResults(ProblemInstance problem, List<AlgorithmResult> results) {
        System.out.println("Vehicle capacity: " + problem.capacity() + " kg");
        for (AlgorithmResult result : results) {
            System.out.println();
            System.out.println(result.algorithm());
            if (result.selectedPackageIndexes().isEmpty()) {
                System.out.println("  Selected packages: none");
            } else {
                System.out.println("  Selected packages:");
                for (int index : result.selectedPackageIndexes()) {
                    SupplyPackage supplyPackage = problem.packages().get(index);
                    System.out.printf("    %d. %s (%d kg, importance %d)%n",
                            index + 1, supplyPackage.name(), supplyPackage.weight(), supplyPackage.importance());
                }
            }
            System.out.printf("  Total weight: %d kg%n", result.totalWeight());
            System.out.printf("  Total importance: %d%n", result.totalImportance());
            System.out.printf("  Runtime: %.3f ms%n", result.runtimeMillis());
        }
    }
}
