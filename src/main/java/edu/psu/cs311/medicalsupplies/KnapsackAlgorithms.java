package edu.psu.cs311.medicalsupplies;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

public final class KnapsackAlgorithms {

    private KnapsackAlgorithms() {
    }

    public static AlgorithmResult solveGreedy(ProblemInstance problem) {
        Objects.requireNonNull(problem, "Problem is required.");
        long startedAt = System.nanoTime();
        List<IndexedPackage> ordered = indexedPackages(problem);
        ordered.sort(KnapsackAlgorithms::compareByRatio);

        List<Integer> selected = new ArrayList<>();
        long remaining = problem.capacity();
        for (IndexedPackage item : ordered) {
            if (item.supplyPackage().weight() <= remaining) {
                selected.add(item.index());
                remaining -= item.supplyPackage().weight();
            }
        }
        return result(problem, "Greedy Ratio Heuristic", selected, startedAt);
    }

    public static AlgorithmResult solveDynamicProgramming(ProblemInstance problem) {
        Objects.requireNonNull(problem, "Problem is required.");
        long startedAt = System.nanoTime();
        int packageCount = problem.packages().size();
        int capacity = problem.capacity();
        long[][] bestImportance = new long[packageCount + 1][capacity + 1];

        for (int row = 1; row <= packageCount; row++) {
            SupplyPackage supplyPackage = problem.packages().get(row - 1);
            for (int limit = 0; limit <= capacity; limit++) {
                long withoutPackage = bestImportance[row - 1][limit];
                if (supplyPackage.weight() > limit) {
                    bestImportance[row][limit] = withoutPackage;
                    continue;
                }

                long withPackage = supplyPackage.importance()
                        + bestImportance[row - 1][limit - supplyPackage.weight()];
                bestImportance[row][limit] = Math.max(withoutPackage, withPackage);
            }
        }

        List<Integer> selected = new ArrayList<>();
        int remaining = capacity;
        for (int row = packageCount; row > 0; row--) {
            if (bestImportance[row][remaining] != bestImportance[row - 1][remaining]) {
                SupplyPackage supplyPackage = problem.packages().get(row - 1);
                selected.add(row - 1);
                remaining -= supplyPackage.weight();
            }
        }

        return result(problem, "Dynamic Programming", selected, startedAt);
    }

    public static AlgorithmResult solveBranchAndBound(ProblemInstance problem) {
        Objects.requireNonNull(problem, "Problem is required.");
        long startedAt = System.nanoTime();
        List<IndexedPackage> ordered = indexedPackages(problem);
        ordered.sort(KnapsackAlgorithms::compareByRatio);

        long sequence = 0;
        PriorityQueue<SearchNode> frontier = new PriorityQueue<>(
                Comparator.comparingLong(SearchNode::upperBound).reversed()
                        .thenComparingLong(SearchNode::sequence));
        long rootBound = upperBound(ordered, 0, 0, 0, problem.capacity());
        frontier.add(new SearchNode(0, 0, 0, rootBound, new BitSet(), sequence++));

        long bestImportance = 0;
        BitSet bestSelection = new BitSet(ordered.size());
        while (!frontier.isEmpty()) {
            SearchNode node = frontier.remove();
            if (node.upperBound() <= bestImportance) {
                continue;
            }

            if (node.importance() > bestImportance) {
                bestImportance = node.importance();
                bestSelection = (BitSet) node.selected().clone();
            }
            if (node.level() == ordered.size()) {
                continue;
            }

            IndexedPackage item = ordered.get(node.level());
            int nextLevel = node.level() + 1;
            SupplyPackage supplyPackage = item.supplyPackage();
            long includedWeight = node.weight() + supplyPackage.weight();

            if (includedWeight <= problem.capacity()) {
                long includedImportance = node.importance() + supplyPackage.importance();
                BitSet includedSelection = (BitSet) node.selected().clone();
                includedSelection.set(item.index());
                if (includedImportance > bestImportance) {
                    bestImportance = includedImportance;
                    bestSelection = (BitSet) includedSelection.clone();
                }

                long includeBound = upperBound(
                        ordered, nextLevel, includedWeight, includedImportance, problem.capacity());
                if (includeBound > bestImportance) {
                    frontier.add(new SearchNode(
                            nextLevel, includedWeight, includedImportance,
                            includeBound, includedSelection, sequence++));
                }
            }

            long excludeBound = upperBound(
                    ordered, nextLevel, node.weight(), node.importance(), problem.capacity());
            if (excludeBound > bestImportance) {
                frontier.add(new SearchNode(
                        nextLevel, node.weight(), node.importance(),
                        excludeBound, (BitSet) node.selected().clone(), sequence++));
            }
        }

        List<Integer> selected = new ArrayList<>();
        for (int index = bestSelection.nextSetBit(0); index >= 0;
             index = bestSelection.nextSetBit(index + 1)) {
            selected.add(index);
        }
        return result(problem, "Branch and Bound", selected, startedAt);
    }

    private static List<IndexedPackage> indexedPackages(ProblemInstance problem) {
        List<IndexedPackage> items = new ArrayList<>();
        for (int index = 0; index < problem.packages().size(); index++) {
            items.add(new IndexedPackage(index, problem.packages().get(index)));
        }
        return items;
    }

    private static int compareByRatio(IndexedPackage left, IndexedPackage right) {
        long leftCrossProduct = (long) left.supplyPackage().importance() * right.supplyPackage().weight();
        long rightCrossProduct = (long) right.supplyPackage().importance() * left.supplyPackage().weight();
        int ratioOrder = Long.compare(rightCrossProduct, leftCrossProduct);
        if (ratioOrder != 0) {
            return ratioOrder;
        }

        int importanceOrder = Integer.compare(
                right.supplyPackage().importance(), left.supplyPackage().importance());
        if (importanceOrder != 0) {
            return importanceOrder;
        }

        int weightOrder = Integer.compare(left.supplyPackage().weight(), right.supplyPackage().weight());
        return weightOrder != 0 ? weightOrder : Integer.compare(left.index(), right.index());
    }

    private static long upperBound(
            List<IndexedPackage> ordered,
            int level,
            long currentWeight,
            long currentImportance,
            int capacity) {
        long remaining = capacity - currentWeight;
        long bound = currentImportance;
        for (int index = level; index < ordered.size(); index++) {
            SupplyPackage supplyPackage = ordered.get(index).supplyPackage();
            if (supplyPackage.weight() <= remaining) {
                remaining -= supplyPackage.weight();
                bound += supplyPackage.importance();
            } else {
                if (remaining > 0) {
                    long fractionalImportance = (long) supplyPackage.importance() * remaining;
                    bound += (fractionalImportance + supplyPackage.weight() - 1L)
                            / supplyPackage.weight();
                }
                break;
            }
        }
        return bound;
    }

    private static AlgorithmResult result(
            ProblemInstance problem,
            String algorithm,
            List<Integer> selectedIndexes,
            long startedAt) {
        selectedIndexes.sort(Integer::compareTo);
        long totalWeight = 0;
        long totalImportance = 0;
        for (int index : selectedIndexes) {
            SupplyPackage supplyPackage = problem.packages().get(index);
            totalWeight += supplyPackage.weight();
            totalImportance += supplyPackage.importance();
        }
        return new AlgorithmResult(
                algorithm,
                selectedIndexes,
                totalWeight,
                totalImportance,
                System.nanoTime() - startedAt);
    }

    private record IndexedPackage(int index, SupplyPackage supplyPackage) {
    }

    private record SearchNode(
            int level,
            long weight,
            long importance,
            long upperBound,
            BitSet selected,
            long sequence) {
    }
}
