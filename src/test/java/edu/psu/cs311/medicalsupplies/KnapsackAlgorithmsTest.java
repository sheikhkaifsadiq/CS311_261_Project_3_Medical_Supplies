package edu.psu.cs311.medicalsupplies;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class KnapsackAlgorithmsTest {

    @Test
    void shouldFindAssignmentSampleOptimumWithBothExactMethods() {
        // Arrange
        ProblemInstance problem = assignmentSample();

        // Act
        AlgorithmResult dynamicProgramming = KnapsackAlgorithms.solveDynamicProgramming(problem);
        AlgorithmResult branchAndBound = KnapsackAlgorithms.solveBranchAndBound(problem);

        // Assert
        assertThat(dynamicProgramming.totalImportance()).isEqualTo(62);
        assertThat(dynamicProgramming.totalWeight()).isEqualTo(12);
        assertThat(dynamicProgramming.selectedPackageIndexes()).containsExactly(0, 1, 3);
        assertThat(branchAndBound.totalImportance()).isEqualTo(62);
        assertThat(branchAndBound.totalWeight()).isEqualTo(12);
        assertThat(branchAndBound.selectedPackageIndexes()).containsExactly(0, 1, 3);
    }

    @Test
    void shouldShowGreedyCanMissTheExactOptimum() {
        // Arrange
        ProblemInstance problem = assignmentSample();

        // Act
        AlgorithmResult greedy = KnapsackAlgorithms.solveGreedy(problem);

        // Assert
        assertThat(greedy.totalImportance()).isEqualTo(50);
        assertThat(greedy.totalWeight()).isEqualTo(9);
        assertThat(greedy.totalImportance())
                .isLessThan(KnapsackAlgorithms.solveDynamicProgramming(problem).totalImportance());
    }

    @Test
    void shouldReturnNoPackagesWhenNothingFits() {
        // Arrange
        ProblemInstance problem = new ProblemInstance(List.of(
                new SupplyPackage("Heavy crate", 8, 20)), 3);

        // Act
        AlgorithmResult result = KnapsackAlgorithms.solveBranchAndBound(problem);

        // Assert
        assertThat(result.selectedPackageIndexes()).isEmpty();
        assertThat(result.totalWeight()).isZero();
        assertThat(result.totalImportance()).isZero();
    }

    @Test
    void shouldKeepDuplicatePackageNamesDistinctByInputPosition() {
        // Arrange
        ProblemInstance problem = new ProblemInstance(List.of(
                new SupplyPackage("Bandages", 4, 10),
                new SupplyPackage("Bandages", 3, 12)), 3);

        // Act
        AlgorithmResult result = KnapsackAlgorithms.solveDynamicProgramming(problem);

        // Assert
        assertThat(result.selectedPackageIndexes()).containsExactly(1);
        assertThat(result.totalImportance()).isEqualTo(12);
    }

    @Test
    void shouldRejectNonPositiveInputValues() {
        // Arrange / Act / Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SupplyPackage("Oxygen", 0, 10));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SupplyPackage("Oxygen", 2, 0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProblemInstance(List.of(), 0));
    }

    @Test
    void shouldValidateExactMethodsAgainstEverySubsetInSmallRandomProblems() {
        // Arrange / Act
        ExhaustiveValidator.ValidationSummary summary = ExhaustiveValidator.validate(100, 12, 311261L);

        // Assert
        assertThat(summary.caseCount()).isEqualTo(100);
        assertThat(summary.subsetsChecked()).isPositive();
    }

    private static ProblemInstance assignmentSample() {
        return new ProblemInstance(List.of(
                new SupplyPackage("First-aid kits", 4, 20),
                new SupplyPackage("Antibiotics", 3, 18),
                new SupplyPackage("Surgical equipment", 6, 25),
                new SupplyPackage("IV fluids", 5, 24),
                new SupplyPackage("Diagnostic kits", 2, 12)), 12);
    }
}
