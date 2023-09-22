package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.EqRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_rptd_path");
        runInstance("instance_2v1");
        runInstance("instance_2v2");
        runInstance("instance_large");
        runInstance("instance_simple");
        runInstance("instance_neighbors_10");
        runInstance("instance_neighbors_12");
        runInstance("instance_random_10");
        runInstance("instance_random_12");
        runInstance("instance_random_20");
        runInstance("instance_random_100");
    }

    private static void runInstance(String instanceName) {
        System.out.println("Running instance: " + instanceName);
        Instance instance = new Instance(instanceName, true);
        CompactModel compactModel = new CompactModel(instance);
        System.out.println("MTZ: " + compactModel.solve());
        ColumnGeneration columnGeneration =
                new ColumnGeneration(new EqRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                        new InitialSolutionHeuristic(instance));
        System.out.println("ColGen: " + columnGeneration.solve());

    }
}
