# Emergency Medical Supply Allocation System

CS311, Term 261, Project Option 3 — Prince Sultan University, Department of Computer Science.

This Java 17 program compares three ways to load indivisible medical supply packages into an emergency vehicle. Each package has a positive integer weight and importance score. The goal is to maximize total importance without exceeding the vehicle capacity.

The importance scores in the sample are synthetic inputs for the assignment. They are not medical triage guidance.

## Requirements

- JDK 17 or later
- Apache Maven 3.9 or later

## Build and run

From this folder, run:

```text
mvn test
mvn package
java -jar target/medical-supply-allocation.jar
```

To use another input file:

```text
java -jar target/medical-supply-allocation.jar path/to/packages.csv
```

The default input is `data/sample.csv`. The program runs Greedy Ratio Heuristic, Dynamic Programming, and Branch and Bound, then prints the selected packages, total weight, total importance, and elapsed time for each method.

## Input format

The file is UTF-8 text. The first non-empty, non-comment line is the vehicle capacity. The next line is the column heading, followed by one package per line:

```text
capacity=12
name,weight,importance
First-aid kits,4,20
Antibiotics,3,18
Surgical equipment,6,25
IV fluids,5,24
Diagnostic kits,2,12
```

Names cannot contain commas. Lines beginning with `#` are ignored. Capacity, weight, and importance must be positive integers. Package names may repeat; the output identifies each selection by its input number.

## Algorithms

- **Greedy Ratio Heuristic:** processes packages by descending importance-to-weight ratio and takes a package only if the whole package fits. It is fast, but can miss the optimum.
- **Dynamic Programming:** solves 0/1 knapsack exactly and reconstructs the selected packages from its table.
- **Branch and Bound:** explores include/exclude choices in order of a fractional-knapsack upper bound. The bound may use part of a package; a final selection never does.

The detailed technical report is supplied separately. Benchmark measurements and runtime charts are included under `results/`. Reproduce the experiments with:

```text
mvn -q -DskipTests package
java -cp target/classes edu.psu.cs311.medicalsupplies.BenchmarkRunner
java -cp target/classes edu.psu.cs311.medicalsupplies.ExhaustiveValidator
```

Benchmark output is written under `results/`; each point has two warm-up runs followed by five timed runs, and the median is recorded. The charts are SVG files and can be opened in a browser or inserted into the report.
