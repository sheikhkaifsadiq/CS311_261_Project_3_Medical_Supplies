package edu.psu.cs311.medicalsupplies;

import java.util.List;

public record AlgorithmResult(
        String algorithm,
        List<Integer> selectedPackageIndexes,
        long totalWeight,
        long totalImportance,
        long runtimeNanos) {

    public AlgorithmResult {
        selectedPackageIndexes = List.copyOf(selectedPackageIndexes);
    }

    public double runtimeMillis() {
        return runtimeNanos / 1_000_000.0;
    }
}
