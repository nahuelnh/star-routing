package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.EqRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;
import ilog.concert.IloException;

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
        try {
            CompactModel compactModel = new CompactModel(instance);
            System.out.println("MTZ: " + compactModel.solve());
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new EqRestrictedMasterProblem(instance),
                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        System.out.println("ColGen: " + columnGeneration.solve());

    }
}
