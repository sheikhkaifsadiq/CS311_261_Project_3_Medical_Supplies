package edu.psu.cs311.medicalsupplies;

import java.util.List;
import java.util.Objects;

public record ProblemInstance(List<SupplyPackage> packages, int capacity) {

    public ProblemInstance {
        Objects.requireNonNull(packages, "Packages are required.");
        packages = List.copyOf(packages);
        if (capacity <= 0) {
            throw new IllegalArgumentException("Vehicle capacity must be a positive integer.");
        }
    }
}
