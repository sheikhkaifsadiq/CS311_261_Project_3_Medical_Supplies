package edu.psu.cs311.medicalsupplies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InputFileReader {

    private InputFileReader() {
    }

    public static ProblemInstance read(Path inputFile) throws IOException {
        List<String> lines = Files.readAllLines(inputFile).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Input file is empty.");
        }

        String capacityLine = lines.get(0);
        int capacity = parseCapacity(capacityLine);
        List<SupplyPackage> packages = new ArrayList<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.toLowerCase().startsWith("name,")) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("Expected name,weight,importance on line "
                        + (lineIndex + 1) + ".");
            }
            try {
                packages.add(new SupplyPackage(
                        fields[0], Integer.parseInt(fields[1].strip()), Integer.parseInt(fields[2].strip())));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Weight and importance must be integers on line "
                        + (lineIndex + 1) + ".", exception);
            }
        }
        return new ProblemInstance(packages, capacity);
    }

    private static int parseCapacity(String line) {
        String value = line.toLowerCase().startsWith("capacity=")
                ? line.substring("capacity=".length()).strip()
                : line;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("First input line must contain an integer capacity.", exception);
        }
    }
}
