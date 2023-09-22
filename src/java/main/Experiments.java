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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Experiments {

    private static final String DELIMITER = ";";
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

    private static String getLine(List<Object> fields) {
        return String.join(DELIMITER, fields.stream().map(String::valueOf).toList());
    }

    private static String getLine(Instance instance, Solution solution) {
        return getLine(List.of(instance.getName(), instance.getNumberOfNodes(), instance.getNumberOfCustomers(),
                instance.getNumberOfVehicles(), solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis()));
    }

    private static void experiment1_compactModelPerformance() {
        System.out.println(getLine(List.of("Name","N","S","K","Time")));
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution = compactModel.solve(TIMEOUT);
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment2_ilpPricingPerformance() {
        System.out.println(getLine(List.of("Name","N","S","K","Time")));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new ILPPricingProblem(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment3_pulsePricingPerformance() {
        System.out.println(getLine(List.of("Name","N","S","K","Time")));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment4_labelSettingPricingPerformance() {
        System.out.println(getLine(List.of("Name","N","S","K","Time")));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve(TIMEOUT);
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment5_labelSettingHeuristics() {
        System.out.println(getLine(List.of("Name","N","S","K","Time std", "Obj std", "Time H", "Obj H", "Gap")));
        for (Instance instance : InstanceEnum.allInstances()) {
            Solution solution =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance)).solve(TIMEOUT);

            List<Object> fields = new ArrayList<>(
                    List.of(instance.getName(), instance.getNumberOfNodes(), instance.getNumberOfCustomers(),
                            instance.getNumberOfVehicles(),
                            solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue()));
            double objValue = solution.getObjValue();

            LabelSettingPricing labelSettingPricing = new LabelSettingPricing(instance);
            labelSettingPricing.applyHeuristics();
            solution = new ColumnGeneration(new GeRestrictedMasterProblem(instance), labelSettingPricing,
                    new InitialSolutionHeuristic(instance)).solve(TIMEOUT);
            double gap = solution.getObjValue() / objValue - 1;

            fields.addAll(List.of(solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue(), gap));

            System.out.println(getLine(fields));
        }
    }

    private static void experiment6_columnGenerationHeuristics() {
        System.out.println(getLine(List.of("Name","N","S","K","Time std", "Obj std", "Time H", "Obj H", "Gap")));
        for (Instance instance : InstanceEnum.allInstances()) {
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            columnGeneration.setApplyFinishEarly(false);
            columnGeneration.setApplyCustomerHeuristic(false);
            Solution solution = columnGeneration.solve(TIMEOUT);
            double objValue = solution.getObjValue();

            List<Object> fields = new ArrayList<>(
                    List.of(instance.getName(), instance.getNumberOfNodes(), instance.getNumberOfCustomers(),
                            instance.getNumberOfVehicles(),
                            solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue()));

            columnGeneration =
                    new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                            new InitialSolutionHeuristic(instance));
            columnGeneration.setApplyFinishEarly(true);
            columnGeneration.setApplyCustomerHeuristic(true);
            solution = columnGeneration.solve(TIMEOUT);
            double gap = solution.getObjValue() / objValue - 1;

            fields.addAll(List.of(solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue(), gap));

            System.out.println(getLine(fields));
        }
    }

    private static void experiment7_relaxationComparison() {
        System.out.println(getLine(List.of("Name","N","S","K","Time compact", "Obj compact", "Time CG", "Obj CG", "Gap")));
        for (Instance instance : InstanceEnum.allInstances()) {
            CompactModel compactModel = new CompactModel(instance);
            Solution solution = compactModel.solve(TIMEOUT);
            List<Object> fields = new ArrayList<>(
                    List.of(instance.getName(), instance.getNumberOfNodes(), instance.getNumberOfCustomers(),
                            instance.getNumberOfVehicles(),
                            solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue()));
            double objValue = solution.getObjValue();
            solution = new ColumnGeneration(new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                    new InitialSolutionHeuristic(instance)).solveRelaxation(TIMEOUT);
            double gap = solution.getObjValue() / objValue - 1;
            fields.addAll(List.of(solution.timedOut() ? "TLE" : solution.getElapsedTime().toMillis(), solution.getObjValue(), gap));
            System.out.println(getLine(fields));
        }
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
