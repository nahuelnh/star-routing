package main;

import algorithm.ColumnGeneration;
import algorithm.FeasibleSolutionHeuristic;
import algorithm.EqRestrictedMasterProblem;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import algorithm.CompactModel;
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
    }

    private static void runInstance(String instanceName) {
        try {
            System.out.println("Running instance: " + instanceName);
            Instance instance = new Instance(instanceName, true);
            CompactModel compactModel = new CompactModel(instance);
            System.out.println("MTZ: " + compactModel.solve());
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new EqRestrictedMasterProblem(instance),
                    new PulsePricing(instance), new FeasibleSolutionHeuristic(instance));
            System.out.println("ColGen: " + columnGeneration.solve());
//            DFJStarRoutingModel dfjStarRoutingModel = new DFJStarRoutingModel(instance);
//            System.out.println("DFJ:" + dfjStarRoutingModel.solve());
//            System.out.println();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }
}
