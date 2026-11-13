package edu.psu.cs311.medicalsupplies;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ExhaustiveValidator {

    private ExhaustiveValidator() {
    }

    public static ValidationSummary validate(int cases, int maxPackages, long seed) {
        if (cases <= 0 || maxPackages < 0 || maxPackages > 20) {
            throw new IllegalArgumentException("Use positive cases and between 0 and 20 packages per case.");
        }

        Random random = new Random(seed);
        long subsetsChecked = 0;
        for (int caseNumber = 1; caseNumber <= cases; caseNumber++) {
            int packageCount = random.nextInt(maxPackages + 1);
            List<SupplyPackage> packages = new ArrayList<>();
            int totalWeight = 0;
            for (int index = 0; index < packageCount; index++) {
                int weight = random.nextInt(18) + 1;
                packages.add(new SupplyPackage("Case" + caseNumber + " package " + (index + 1),
                        weight, random.nextInt(45) + 1));
                totalWeight += weight;
            }

            int capacity = packageCount == 0 ? 1 : random.nextInt(totalWeight) + 1;
            ProblemInstance problem = new ProblemInstance(packages, capacity);
            long expected = exhaustiveOptimum(problem);
            subsetsChecked += 1L << packageCount;

            AlgorithmResult dynamicProgramming = KnapsackAlgorithms.solveDynamicProgramming(problem);
            AlgorithmResult branchAndBound = KnapsackAlgorithms.solveBranchAndBound(problem);
            checkResult(problem, dynamicProgramming, expected, caseNumber);
            checkResult(problem, branchAndBound, expected, caseNumber);
        }
        return new ValidationSummary(cases, maxPackages, seed, subsetsChecked);
    }

    static long exhaustiveOptimum(ProblemInstance problem) {
        int packageCount = problem.packages().size();
        long bestImportance = 0;
        long subsetCount = 1L << packageCount;
        for (long mask = 0; mask < subsetCount; mask++) {
            long totalWeight = 0;
            long totalImportance = 0;
            for (int index = 0; index < packageCount; index++) {
                if ((mask & (1L << index)) != 0) {
                    SupplyPackage supplyPackage = problem.packages().get(index);
                    totalWeight += supplyPackage.weight();
                    if (totalWeight > problem.capacity()) {
                        break;
                    }
                    totalImportance += supplyPackage.importance();
                }
            }
            if (totalWeight <= problem.capacity()) {
                bestImportance = Math.max(bestImportance, totalImportance);
            }
        }
        return bestImportance;
    }

    private static void checkResult(
            ProblemInstance problem, AlgorithmResult result, long expected, int caseNumber) {
        if (result.totalImportance() != expected) {
            throw new IllegalStateException(result.algorithm() + " returned " + result.totalImportance()
                    + " instead of " + expected + " in validation case " + caseNumber + ".");
        }
        if (result.totalWeight() > problem.capacity()) {
            throw new IllegalStateException(result.algorithm() + " exceeded capacity in case " + caseNumber + ".");
        }

        long selectedWeight = 0;
        long selectedImportance = 0;
        for (int index : result.selectedPackageIndexes()) {
            SupplyPackage supplyPackage = problem.packages().get(index);
            selectedWeight += supplyPackage.weight();
            selectedImportance += supplyPackage.importance();
        }
        if (selectedWeight != result.totalWeight() || selectedImportance != result.totalImportance()) {
            throw new IllegalStateException(result.algorithm() + " returned inconsistent totals in case "
                    + caseNumber + ".");
        }
    }

    public static void main(String[] args) {
        run(150, 15, 311261L);
    }

    public static void run(int cases, int maxPackages, long seed) {
        ValidationSummary summary = validate(cases, maxPackages, seed);
        System.out.printf("Validated %d cases; enumerated %,d subsets (seed %d).%n",
                summary.caseCount(), summary.subsetsChecked(), summary.seed());
    }

    public record ValidationSummary(int caseCount, int maxPackages, long seed, long subsetsChecked) {
    }
}
