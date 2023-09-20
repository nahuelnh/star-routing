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
import ilog.concert.IloException;

import java.time.Duration;

public class Experiments {

    private static final String DELIMITER = ";";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public static void main(String[] args) {
        experiment1_compactModelPerformance();
        experiment2_ilpPricingPerformance();
        experiment3_pulsePricingPerformance();
        experiment4_labelSettingPricingPerformance();
        experiment5_labelSettingHeuristics();
        experiment6_columnGenerationHeuristics();
    }

    private static String getLine(Instance instance, Solution solution) {
        return String.join(DELIMITER, instance.getName(), String.valueOf(instance.getNumberOfNodes()),
                String.valueOf(instance.getNumberOfCustomers()), String.valueOf(instance.getNumberOfVehicles()),
                solution.timedOut() ? "TLE" : String.valueOf(solution.getElapsedTime().toMillis()));
    }
    private static String getLine(Instance instance, Solution solution, double gap) {
        return String.join(DELIMITER, instance.getName(), String.valueOf(instance.getNumberOfNodes()),
                String.valueOf(instance.getNumberOfCustomers()), String.valueOf(instance.getNumberOfVehicles()),
                solution.timedOut() ? "TLE" : String.valueOf(solution.getElapsedTime().toMillis()), String.valueOf(gap));
    }


    private static void experiment1_compactModelPerformance() {
        for (InstanceEnum instanceEnum : InstanceEnum.values()) {
            try {
                Instance instance = instanceEnum.getInstance();
                CompactModel compactModel = new CompactModel(instance);
                Solution solution = compactModel.solve(TIMEOUT);
                System.out.println(getLine(instance, solution));
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void experiment2_ilpPricingPerformance() {
        for (InstanceEnum instanceEnum : InstanceEnum.values()) {
            Instance instance = instanceEnum.getInstance();
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new ILPPricingProblem(instance), new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve();
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment3_pulsePricingPerformance() {
        for (InstanceEnum instanceEnum : InstanceEnum.values()) {
            Instance instance = instanceEnum.getInstance();
            ColumnGeneration columnGeneration =
                    new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                            new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve();
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment4_labelSettingPricingPerformance() {
        for (InstanceEnum instanceEnum : InstanceEnum.values()) {
            Instance instance = instanceEnum.getInstance();
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            Solution solution = columnGeneration.solve();
            System.out.println(getLine(instance, solution));
        }
    }

    private static void experiment5_labelSettingHeuristics() {

    }

    private static void experiment6_columnGenerationHeuristics() {

    }

    private static void experiment7_relaxationComparison() {

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

        public Instance getInstance() {
            return instance;
        }
    }
}
