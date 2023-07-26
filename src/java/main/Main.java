package main;

import algorithm.ColumnGeneration;
import algorithm.ESPPRCPricing;
import algorithm.FeasibleSolutionHeuristic;
import algorithm.MTZRestrictedMasterProblem;
import algorithm.SecondPricingProblem;
import algorithm.StarRoutingModel;
import commons.Instance;
import ilog.concert.IloException;

public class Main {
    public static void main(String[] args) {
//        runInstance("instance_rptd_path");
//        runInstance("instance_2v1");
//        runInstance("instance_2v2");
//        runInstance("instance_large");
//        runInstance("instance_simple");
        runInstance("instance_neighbors_10");
        runInstance("instance_neighbors_12");
        runInstance("instance_random_10");
        runInstance("instance_random_12");
    }

    private static void runInstance(String instanceName) {
        try {
            System.out.println("Running instance: " + instanceName);
            Instance instance = new Instance(instanceName, true);
            StarRoutingModel starRoutingModel = new StarRoutingModel(instance);
            System.out.println(starRoutingModel.solve());
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new MTZRestrictedMasterProblem(instance),
                    new ESPPRCPricing(instance), new FeasibleSolutionHeuristic(instance));
            System.out.println(columnGeneration.solve());
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }
}
