package main;

import algorithm.ColumnGenerator;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.ILPPricingProblem;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.InstanceLoader;
import commons.StarRoutingSolution;
import commons.Table;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

public class Experiments {

    private static final String DEFAULT_FIELD = "---";
    private static final String TIME_LIMIT_EXCEEDED = "TLE";
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.##");
    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    public static void main(String[] args) {
//        experiment1_compactModelPerformance();
//        experiment2_ilpPricingPerformance();
//        experiment3_pulsePricingPerformance();
        experiment4_labelSettingPricingPerformance();
//        experiment5_labelSettingHeuristics();
//        experiment6_columnGenerationHeuristics();
//        experiment7_columnGenerationFinishEarly();
//        experiment8_relaxationComparison();
    }

    private static double gapAsPercent(double value, double lowerBound) {
        return 100.0 * (value / lowerBound - 1);
    }

    private static void experiment1_compactModelPerformance() {
        Table table =
                new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms)", "#Ticks"), true, "experiment1.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            if (instance.getNumberOfNodes() <= 20) {
                CompactModel compactModel = new CompactModel(instance);
                StarRoutingSolution solution = compactModel.solve(TIMEOUT);
                table.addEntry(new SimpleTableEntry(instance, solution));
                if (solution.timedOut()) {
                    unfinishedInstances++;
                } else {
                    unfinishedInstances = 0;
                }
                if (unfinishedInstances == 3) {
                    break;
                }
            }
        }
        table.close();
    }

    private static void experiment2_ilpPricingPerformance() {
        Table table =
                new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms)", "#Ticks", "#Iter GC", "Gap"), true,
                        "experiment2.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGenerator columnGenerator = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new ILPPricingProblem(instance), new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution = columnGenerator.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGenerator));
            if (solution.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static void experiment3_pulsePricingPerformance() {
        Table table =
                new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms)", "#Pulses", "#Iter GC", "Gap"), true,
                        "experiment3.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGenerator columnGenerator =
                    new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution = columnGenerator.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGenerator));
            if (solution.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static void experiment4_labelSettingPricingPerformance() {
        Table table =
                new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms)", "#Labels", "#Iter GC", "Gap"), true,
                        "experiment4.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGenerator columnGenerator = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution = columnGenerator.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGenerator));
            if (solution.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static void experiment5_labelSettingHeuristics() {
        Table table = new Table(
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms) s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo (ms) c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap Exacto", "Cota Inferior",
                        "Gap Aprox."), true, "experiment5.csv");

        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {

            ColumnGenerator columnGenerator1 = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution1 = columnGenerator1.solve(TIMEOUT);

            LabelSettingPricing labelSettingPricing = new LabelSettingPricing(instance, true);
            ColumnGenerator columnGenerator2 =
                    new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance), labelSettingPricing,
                            new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution2 = columnGenerator2.solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGenerator1, columnGenerator2));
            if (solution1.timedOut() && solution2.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }

        }
        table.close();
    }

    private static void experiment6_columnGenerationHeuristics() {
        Table table = new Table(
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms) s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo (ms) c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap Exacto", "Cota Inferior",
                        "Gap Aprox."), true, "experiment6.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGenerator columnGenerator1 = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance, true), new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution1 = columnGenerator1.solve(TIMEOUT);

            ColumnGenerator columnGenerator2 = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance, true), new InitialSolutionHeuristic(instance));
            columnGenerator2.applyRearrangeCustomersHeuristic();
            StarRoutingSolution solution2 = columnGenerator2.solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGenerator1, columnGenerator2));
            if (solution1.timedOut() && solution2.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static void experiment7_columnGenerationFinishEarly() {
        Table table = new Table(
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms) s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo (ms) c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap Exacto", "Cota Inferior",
                        "Gap Aprox."), true, "experiment7.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGenerator columnGenerator1 =
                    new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution1 = columnGenerator1.solve(TIMEOUT);

            ColumnGenerator columnGenerator2 =
                    new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            columnGenerator2.finishEarly(0.05);
            StarRoutingSolution solution2 = columnGenerator2.solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGenerator1, columnGenerator2));
            if (solution1.timedOut() && solution2.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static void experiment8_relaxationComparison() {
        Table table = new Table(
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo (ms) MC", "Obj. RL MC", "Tiempo (ms) GC",
                        "Obj. RL GC", "#Iter GC", "Gap"), true, "experiment8.csv");
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            StarRoutingSolution solution1 = compactModel.solveRelaxation(TIMEOUT);
            ColumnGenerator columnGenerator = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            StarRoutingSolution solution2 = columnGenerator.solveRelaxation(TIMEOUT);
            table.addEntry(new RelaxationComparisonTableEntry(instance, solution1, solution2, columnGenerator));
            if (solution1.timedOut() && solution2.timedOut()) {
                unfinishedInstances++;
            } else {
                unfinishedInstances = 0;
            }
            if (unfinishedInstances == 3) {
                break;
            }
        }
        table.close();
    }

    private static String getInstanceName(Instance instance) {
        return instance.getName();
    }

    private static String getNumberOfNodes(Instance instance) {
        return String.valueOf(instance.getNumberOfNodes());
    }

    private static String getNumberOfCustomers(Instance instance) {
        return String.valueOf(instance.getNumberOfCustomers());
    }

    private static String getNumberOfVehicles(Instance instance) {
        return String.valueOf(instance.getNumberOfVehicles());
    }

    private static String getElapsedTime(StarRoutingSolution solution) {
        return solution.timedOut() ? TIME_LIMIT_EXCEEDED : String.valueOf(solution.getElapsedTime().toMillis());
    }

    private static String getDeterministicTime(StarRoutingSolution solution) {
        return solution.hasDeterministicTime() ? FORMATTER.format(solution.getDeterministicTime()) : DEFAULT_FIELD;
    }

    private static String getNumberOfIterations(ColumnGenerator columnGenerator) {
        return String.valueOf(columnGenerator.getNumberOfIterations());
    }

    private static String getObjValue(StarRoutingSolution solution) {
        return FORMATTER.format(solution.getObjValue());
    }

    private static String getGapBetweenSolutions(StarRoutingSolution solution1, StarRoutingSolution solution2) {
        return !solution1.timedOut() && !solution2.timedOut() ?
                FORMATTER.format(gapAsPercent(solution1.getObjValue(), solution2.getObjValue())) + "%" : DEFAULT_FIELD;
    }

    private static String getGapToLowerBound(StarRoutingSolution solution) {
        return solution.hasLowerBound() ?
                FORMATTER.format(gapAsPercent(solution.getObjValue(), solution.getLowerBound())) + "%" : DEFAULT_FIELD;
    }

    private static String getLowerBound(StarRoutingSolution solution) {
        return solution.hasLowerBound() ? FORMATTER.format(solution.getLowerBound()) : DEFAULT_FIELD;
    }

    private static class SimpleTableEntry implements Table.Entry {

        private final Instance instance;
        private final StarRoutingSolution solution;

        public SimpleTableEntry(Instance instance, StarRoutingSolution solution) {
            this.instance = instance;
            this.solution = solution;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution), getDeterministicTime(solution));
        }
    }

    private static class ExtendedTableEntry implements Table.Entry {

        private final Instance instance;
        private final StarRoutingSolution solution;
        private final ColumnGenerator columnGenerator;

        public ExtendedTableEntry(Instance instance, StarRoutingSolution solution, ColumnGenerator columnGenerator) {
            this.instance = instance;
            this.solution = solution;
            this.columnGenerator = columnGenerator;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution), getDeterministicTime(solution),
                    getNumberOfIterations(columnGenerator), getGapToLowerBound(solution));
        }
    }

    private static class ComparisonTableEntry implements Table.Entry {

        private final Instance instance;
        private final StarRoutingSolution solution1;
        private final StarRoutingSolution solution2;
        private final ColumnGenerator columnGenerator1;
        private final ColumnGenerator columnGenerator2;

        public ComparisonTableEntry(Instance instance, StarRoutingSolution solution, StarRoutingSolution solution2,
                                    ColumnGenerator columnGenerator1, ColumnGenerator columnGenerator2) {
            this.instance = instance;
            this.solution1 = solution;
            this.solution2 = solution2;
            this.columnGenerator1 = columnGenerator1;
            this.columnGenerator2 = columnGenerator2;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution1), getObjValue(solution1),
                    getNumberOfIterations(columnGenerator1), getElapsedTime(solution2), getObjValue(solution2),
                    getNumberOfIterations(columnGenerator2), getGapBetweenSolutions(solution2, solution1),
                    getLowerBound(solution2), getGapToLowerBound(solution2));

        }
    }

    private static class RelaxationComparisonTableEntry implements Table.Entry {

        private final Instance instance;
        private final StarRoutingSolution solution1;
        private final StarRoutingSolution solution2;
        private final ColumnGenerator columnGenerator;

        public RelaxationComparisonTableEntry(Instance instance, StarRoutingSolution solution,
                                              StarRoutingSolution solution2, ColumnGenerator columnGenerator) {
            this.instance = instance;
            this.solution1 = solution;
            this.solution2 = solution2;
            this.columnGenerator = columnGenerator;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution1), getObjValue(solution1),
                    getElapsedTime(solution2), getObjValue(solution2), getNumberOfIterations(columnGenerator),
                    getGapBetweenSolutions(solution1, solution2));
        }
    }
}

