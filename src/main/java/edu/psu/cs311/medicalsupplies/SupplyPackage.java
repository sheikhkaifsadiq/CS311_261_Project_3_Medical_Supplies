package edu.psu.cs311.medicalsupplies;

import java.util.Objects;

public record SupplyPackage(String name, int weight, int importance) {

    public SupplyPackage {
        Objects.requireNonNull(name, "Package name is required.");
        name = name.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be blank.");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Package weight must be a positive integer.");
        }
        if (importance <= 0) {
            throw new IllegalArgumentException("Package importance must be a positive integer.");
        }
    }
}
