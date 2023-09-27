package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_n9_s4_k2");
    }

    private static void runInstance(String instanceName) {
        System.out.println("Running instance: " + instanceName);
        Instance instance = new Instance(instanceName, true);
        CompactModel compactModel = new CompactModel(instance);
        System.out.println("MTZ: " + compactModel.solve());
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        columnGeneration.applyRearrangeCustomersHeuristic();
        System.out.println("ColGen: " + columnGeneration.solve());

    }
}
