package main;

import algorithm.ColumnGeneration;
import algorithm.CompactModel;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;
import ilog.concert.IloException;

public class Experiments {

    public static void main(String[] args) {
        Instance instance = new Instance("instance_random_10", true);
        experiment1(instance);
    }

    private static void experiment1(Instance instance) {
        try {
            CompactModel compactModel = new CompactModel(instance);
            System.out.println("Exact:\n" + compactModel.solve());
            System.out.println("Compact Relax: " + compactModel.solveRelaxation().getObjValue());
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        System.out.println("CG Relax: " + columnGeneration.solveRelaxation().getObjValue());

    }
}
