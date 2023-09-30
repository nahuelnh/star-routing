package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.ILPPricingProblem;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.InstanceLoader;
import commons.Solution;
import commons.Table;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

public class Experiments {

    private static final String OUTPUT_FILE = "experiments.csv";
    private static final String DEFAULT_FIELD = "---";
    private static final String TIME_LIMIT_EXCEEDED = "TLE";
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.##");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    public static void main(String[] args) {
        // TODO: possible experiments: DFJ vs MTZ, Eq vs Ge RMP

        //        experiment1_compactModelPerformance();
        //        experiment2_ilpPricingPerformance();
        //        experiment3_pulsePricingPerformance();
        experiment4_labelSettingPricingPerformance();
        //        experiment5_labelSettingHeuristics();
        //        experiment6_columnGenerationHeuristics();
        //        experiment7_relaxationComparison();
    }

    private static double gapAsPercent(double value, double lowerBound) {
        return 100.0 * (value / lowerBound - 1);
    }

    private static void experiment1_compactModelPerformance() {
        Table table = new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks"), true, OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution = compactModel.solve(TIMEOUT);
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
        table.close();
    }

    private static void experiment2_ilpPricingPerformance() {
        Table table = new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks", "#Iter GC", "Gap"), true,
                OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new ILPPricingProblem(instance), new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
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
        Table table = new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Pulses", "#Iter GC", "Gap"), true,
                OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
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
        Table table = new Table(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Labels", "#Iter GC", "Gap"), true,
                OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
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
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap"), true, OUTPUT_FILE);

        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {

            ColumnGeneration columnGeneration1 = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            Solution solution1 = columnGeneration1.solve(TIMEOUT);

            LabelSettingPricing labelSettingPricing = new LabelSettingPricing(instance, true);
            ColumnGeneration columnGeneration2 =
                    new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), labelSettingPricing,
                            new InitialSolutionHeuristic(instance));
            Solution solution2 =
                    new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), labelSettingPricing,
                            new InitialSolutionHeuristic(instance)).solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGeneration1, columnGeneration2));
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
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap"), true, OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            ColumnGeneration columnGeneration1 = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            Solution solution1 = columnGeneration1.solve(TIMEOUT);

            ColumnGeneration columnGeneration2 = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            columnGeneration2.applyRearrangeCustomersHeuristic();
            Solution solution2 = columnGeneration2.solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGeneration1, columnGeneration2));
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

    private static void experiment7_relaxationComparison() {
        Table table = new Table(
                List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo MC", "Obj. RL MC", "Tiempo GC", "Obj. RL GC", "Gap"),
                true, OUTPUT_FILE);
        int unfinishedInstances = 0;
        for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution1 = compactModel.solve(TIMEOUT);
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            Solution solution2 = columnGeneration.solveRelaxation(TIMEOUT);
            table.addEntry(new RelaxationComparisonTableEntry(instance, solution1, solution2, columnGeneration));
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

    private static String getElapsedTime(Solution solution) {
        return solution.timedOut() ? TIME_LIMIT_EXCEEDED : (solution.getElapsedTime().toMillis() + "ms");
    }

    private static String getDeterministicTime(Solution solution) {
        return solution.hasDeterministicTime() ? FORMATTER.format(solution.getDeterministicTime()) : DEFAULT_FIELD;
    }

    private static String getNumberOfIterations(ColumnGeneration columnGeneration) {
        return String.valueOf(columnGeneration.getNumberOfIterations());
    }

    private static String getObjValue(Solution solution) {
        return FORMATTER.format(solution.getObjValue());
    }

    private static String getGapBetweenSolutions(Solution solution1, Solution solution2) {
        return FORMATTER.format(gapAsPercent(solution1.getObjValue(), solution2.getObjValue())) + "%";
    }

    private static String getGapToLowerBound(Solution solution) {
        return solution.hasLowerBound() ?
                FORMATTER.format(gapAsPercent(solution.getObjValue(), solution.getLowerBound())) + "%" : DEFAULT_FIELD;
    }

    private static class SimpleTableEntry implements Table.Entry {

        private final Instance instance;
        private final Solution solution;

        public SimpleTableEntry(Instance instance, Solution solution) {
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
        private final Solution solution;
        private final ColumnGeneration columnGeneration;

        public ExtendedTableEntry(Instance instance, Solution solution, ColumnGeneration columnGeneration) {
            this.instance = instance;
            this.solution = solution;
            this.columnGeneration = columnGeneration;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution), getDeterministicTime(solution),
                    getNumberOfIterations(columnGeneration), getGapToLowerBound(solution));
        }
    }

    private static class ComparisonTableEntry implements Table.Entry {

        private final Instance instance;
        private final Solution solution1;
        private final Solution solution2;
        private final ColumnGeneration columnGeneration1;
        private final ColumnGeneration columnGeneration2;

        public ComparisonTableEntry(Instance instance, Solution solution, Solution solution2,
                                    ColumnGeneration columnGeneration1, ColumnGeneration columnGeneration2) {
            this.instance = instance;
            this.solution1 = solution;
            this.solution2 = solution2;
            this.columnGeneration1 = columnGeneration1;
            this.columnGeneration2 = columnGeneration2;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution1), getObjValue(solution1),
                    getNumberOfIterations(columnGeneration1), getElapsedTime(solution2), getObjValue(solution2),
                    getNumberOfIterations(columnGeneration2), getGapBetweenSolutions(solution1, solution2));

        }
    }

    private static class RelaxationComparisonTableEntry implements Table.Entry {

        private final Instance instance;
        private final Solution solution1;
        private final Solution solution2;
        private final ColumnGeneration columnGeneration;

        public RelaxationComparisonTableEntry(Instance instance, Solution solution, Solution solution2,
                                              ColumnGeneration columnGeneration) {
            this.instance = instance;
            this.solution1 = solution;
            this.solution2 = solution2;
            this.columnGeneration = columnGeneration;
        }

        @Override
        public List<String> getFields() {
            return List.of(getInstanceName(instance), getNumberOfNodes(instance), getNumberOfCustomers(instance),
                    getNumberOfVehicles(instance), getElapsedTime(solution1), getObjValue(solution1),
                    getElapsedTime(solution2), getObjValue(solution2), getNumberOfIterations(columnGeneration),
                    getGapBetweenSolutions(solution1, solution2));
        }
    }
}

