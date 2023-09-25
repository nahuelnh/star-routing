package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.ILPPricingProblem;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.InstanceEnum;
import commons.Solution;
import commons.Table;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

public class Experiments {

    private static final String DEFAULT_FIELD = "---";
    private static final String TIME_LIMIT_EXCEEDED = "TLE";
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.##");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public static void main(String[] args) {
        // TODO: possible experiments: DFJ vs MTZ, Eq vs Ge RMP
        // TODO: det time, lower bounds, gap quality, triangle inequality instances

        experiment1_compactModelPerformance();
        System.out.println("------------------------------");

        experiment2_ilpPricingPerformance();
        System.out.println("------------------------------");

        experiment3_pulsePricingPerformance();
        System.out.println("------------------------------");

        experiment4_labelSettingPricingPerformance();
        System.out.println("------------------------------");

        experiment5_labelSettingHeuristics();
        System.out.println("------------------------------");

        experiment6_columnGenerationHeuristics();
        System.out.println("------------------------------");

        experiment7_relaxationComparison();
    }

    private static double gapAsPercent(double value, double lowerBound) {
        return 100.0 * (value / lowerBound - 1);
    }

    private static void experiment1_compactModelPerformance() {
        Table table = Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks");
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution = compactModel.solve(TIMEOUT);
            table.addEntry(new SimpleTableEntry(instance, solution));
        }
        table.print();
    }

    private static void experiment2_ilpPricingPerformance() {
        Table table = Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks", "#Iter GC", "Sol. Óptima");
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new ILPPricingProblem(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
        }
        table.print();
    }

    private static void experiment3_pulsePricingPerformance() {
        Table table = Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Pulses", "#Iter GC", "Sol. Óptima");

        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
        }
        table.print();

    }

    private static void experiment4_labelSettingPricingPerformance() {
        Table table = Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Labels", "#Iter GC", "Sol. Óptima");

        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            table.addEntry(new ExtendedTableEntry(instance, solution, columnGeneration));
        }
        table.print();
    }

    private static void experiment5_labelSettingHeuristics() {
        Table table =
                Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap");

        for (Instance instance : InstanceEnum.allInstances()) {

            ColumnGeneration columnGeneration1 =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution1 = columnGeneration1.solve(TIMEOUT);

            LabelSettingPricing labelSettingPricing = new LabelSettingPricing(instance);
            labelSettingPricing.solveHeuristically();
            ColumnGeneration columnGeneration2 =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), labelSettingPricing,
                            new InitialSolutionHeuristic(instance));

            Solution solution2 = new ColumnGeneration(new GeRestrictedMasterProblem(instance), labelSettingPricing,
                    new InitialSolutionHeuristic(instance)).solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGeneration1, columnGeneration2));

        }
        table.print();
    }

    private static void experiment6_columnGenerationHeuristics() {

        Table table =
                Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Óptima", "#Iter GC s/Heur.",
                        "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap");
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration1 =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            columnGeneration1.setApplyFinishEarly(false);
            columnGeneration1.setApplyCustomerHeuristic(false);
            Solution solution1 = columnGeneration1.solve(TIMEOUT);

            ColumnGeneration columnGeneration2 =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            columnGeneration2.setApplyFinishEarly(true);
            columnGeneration2.setApplyCustomerHeuristic(true);
            Solution solution2 = columnGeneration2.solve(TIMEOUT);

            table.addEntry(
                    new ComparisonTableEntry(instance, solution1, solution2, columnGeneration1, columnGeneration2));
        }
        table.print();
    }

    private static void experiment7_relaxationComparison() {
        Table table =
                Table.ofHeaders("Instancia", "|N|", "|S|", "|K|", "Tiempo MC", "Obj. RL MC", "Tiempo GC", "Obj. RL GC",
                        "Gap");
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution1 = compactModel.solve(TIMEOUT);
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution2 = columnGeneration.solveRelaxation(TIMEOUT);
            table.addEntry(new RelaxationComparisonTableEntry(instance, solution1, solution2, columnGeneration));
        }
        table.print();
    }

    private static String instance(Instance instance) {
        return instance.getName();
    }

    private static String N(Instance instance) {
        return String.valueOf(instance.getNumberOfNodes());
    }

    private static String S(Instance instance) {
        return String.valueOf(instance.getNumberOfCustomers());

    }

    private static String K(Instance instance) {
        return String.valueOf(instance.getNumberOfVehicles());

    }

    private static String elapsedTime(Solution solution) {
        return solution.timedOut() ? TIME_LIMIT_EXCEEDED : String.valueOf(solution.getElapsedTime().toMillis());

    }

    private static String deterministicTime(Solution solution) {
        return solution.hasDeterministicTime() ? FORMATTER.format(solution.getDeterministicTime()) : DEFAULT_FIELD;

    }

    private static String numberOfIterations(ColumnGeneration columnGeneration) {
        return String.valueOf(columnGeneration.getNumberOfIterations());

    }

    private static String objValue(Solution solution) {
        return FORMATTER.format(solution.getObjValue());

    }

    private static String gapToOtherSol(Solution solution1, Solution solution2) {
        return FORMATTER.format(gapAsPercent(solution1.getObjValue(), solution2.getObjValue())) + "%";

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
            return List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution));
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
            return List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution), numberOfIterations(columnGeneration), objValue(solution));
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
            return List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution1),
                    objValue(solution1), numberOfIterations(columnGeneration1), elapsedTime(solution2),
                    objValue(solution2), numberOfIterations(columnGeneration2), gapToOtherSol(solution1, solution2));

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
            return List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution1),
                    objValue(solution1), elapsedTime(solution2), objValue(solution2),
                    numberOfIterations(columnGeneration), gapToOtherSol(solution1, solution2));

        }
    }

}

