package edu.psu.cs311.medicalsupplies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InputFileReaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldReadCapacityAndPackageRows() throws IOException {
        // Arrange
        Path input = temporaryDirectory.resolve("supplies.csv");
        Files.writeString(input, "capacity=7\nname,weight,importance\nGauze,2,8\nSaline,5,12\n");

        // Act
        ProblemInstance problem = InputFileReader.read(input);

        // Assert
        assertThat(problem.capacity()).isEqualTo(7);
        assertThat(problem.packages()).containsExactly(
                new SupplyPackage("Gauze", 2, 8), new SupplyPackage("Saline", 5, 12));
    }

    @Test
    void shouldRejectMalformedPackageRows() throws IOException {
        // Arrange
        Path input = temporaryDirectory.resolve("invalid.csv");
        Files.writeString(input, "capacity=7\nBandages,2\n");

        // Act / Assert
        assertThatIllegalArgumentException().isThrownBy(() -> InputFileReader.read(input))
                .withMessageContaining("Expected name,weight,importance");
    }
}
