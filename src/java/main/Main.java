package main;

import algorithm.ColumnGeneration;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;
import commons.Solution;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_n16_s3_k2");
    }

    private static void runInstance(String instanceName) {
        System.out.println("Running instance: " + instanceName);
        Instance instance = new Instance(instanceName, true);
        //        CompactModel compactModel = new CompactModel(instance);
        //        System.out.println("MTZ: " + compactModel.solve());
        LabelSettingPricing labelSettingPricing = new LabelSettingPricing(instance);
        ColumnGeneration columnGeneration =
                new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), labelSettingPricing,
                        new InitialSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
        System.out.println("ColGen: " + solution);
        System.out.println(solution.getLowerBound());
        System.out.println(columnGeneration.getNumberOfIterations());

    }
}
