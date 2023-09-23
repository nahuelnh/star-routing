package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.ILPPricingProblem;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.Solution;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class Experiments {

    private static final String DELIMITER = ";";
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
        return solution.timedOut() ? TIME_LIMIT_EXCEEDED : FORMATTER.format(solution.getElapsedTime().toMillis());
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

    private static double gap(double objValue, double lowerBound) {
        return 100.0 * (objValue / lowerBound - 1);
    }

    private static String gapToLowerBound(Solution solution) {
        return solution.hasLowerBound() ?
                FORMATTER.format(gap(solution.getObjValue(), solution.getLowerBound())) + "%" : DEFAULT_FIELD;
    }

    private static String gapToOtherSol(Solution solution1, Solution solution2) {
        return FORMATTER.format(gap(solution1.getObjValue(), solution2.getObjValue())) + "%";
    }

    private static void experiment1_compactModelPerformance() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks"));
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution = compactModel.solve(TIMEOUT);
            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution)));
        }
    }

    private static void experiment2_ilpPricingPerformance() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Ticks", "#Iter. GC", "Valor Obj.",
                "Cota Inferior", "Gap"));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new ILPPricingProblem(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution), numberOfIterations(columnGeneration), objValue(solution)));
        }
    }

    private static void experiment3_pulsePricingPerformance() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Pulses", "#Iter. GC", "Valor Obj.",
                "Cota Inferior", "Gap"));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution), numberOfIterations(columnGeneration), objValue(solution)));
        }
    }

    private static void experiment4_labelSettingPricingPerformance() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo", "#Labels", "#Iter. GC", "Valor Obj.",
                "Cota Inferior", "Gap"));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution),
                    deterministicTime(solution), numberOfIterations(columnGeneration), objValue(solution)));
        }
    }

    private static void experiment5_labelSettingHeuristics() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Optima", "#Iter GC s/Heur.",
                "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap"));

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

            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution1),
                    objValue(solution1), numberOfIterations(columnGeneration1), elapsedTime(solution2),
                    objValue(solution2), numberOfIterations(columnGeneration2), gapToOtherSol(solution1, solution2)));

        }
    }

    private static void experiment6_columnGenerationHeuristics() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo s/Heur.", "Sol. Optima", "#Iter GC s/Heur.",
                "Tiempo c/Heur.", "Sol. Aprox.", "#Iter GC c/Heur.", "Gap"));
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

            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution1),
                    objValue(solution1), numberOfIterations(columnGeneration1), elapsedTime(solution2),
                    objValue(solution2), numberOfIterations(columnGeneration2), gapToOtherSol(solution1, solution2)));
        }
    }

    private static void experiment7_relaxationComparison() {
        printLine(List.of("Instancia", "|N|", "|S|", "|K|", "Tiempo MC", "Obj. RL MC", "Tiempo GC.", "Obj. RL GC",
                "#Iter GC", "Gap"));
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution1 = compactModel.solve(TIMEOUT);

            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution2 = columnGeneration.solveRelaxation(TIMEOUT);

            printLine(List.of(instance(instance), N(instance), S(instance), K(instance), elapsedTime(solution1),
                    objValue(solution1), elapsedTime(solution2), objValue(solution2),
                    numberOfIterations(columnGeneration), gapToOtherSol(solution1, solution2)));
        }
    }

    private static void printLine(List<String> fields) {
        System.out.println(String.join(DELIMITER, fields));
    }

    private enum InstanceEnum {

        SIMPLE("instance_simple"),
        LARGE("instance_large"),
        RANDOM_10("instance_random_10"),
        RANDOM_20("instance_random_20");

        private final Instance instance;

        InstanceEnum(String instanceName) {
            this.instance = new Instance(instanceName, true);
        }

        public static List<Instance> allInstances() {
            return Arrays.stream(values()).map(InstanceEnum::getInstance).toList();
        }

        public Instance getInstance() {
            return instance;
        }
    }
}
