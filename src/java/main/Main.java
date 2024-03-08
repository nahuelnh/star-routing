package main;

import algorithm.BranchAndPrice;
import algorithm.ColumnGeneration;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import commons.Instance;
import commons.Solution;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_n16_s8_k2");
    }

    private static void runInstance(String instanceName) {
        System.out.println("Running instance: " + instanceName);
        Instance instance = new Instance(instanceName, true);
        //        CompactModel compactModel = new CompactModel(instance);
        //        System.out.println("MTZ: " + compactModel.solve());

        //         columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
        //                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        //        columnGeneration.finishEarly(0.05);
        //         solution = columnGeneration.solve();
        //        System.out.println("ColGen: " + solution);
        //        System.out.println(solution.getLowerBound());
        //        System.out.println(columnGeneration.getNumberOfIterations());
        //
        //        columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
        //                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        //        columnGeneration.applyRearrangeCustomersHeuristic();
        //        solution = columnGeneration.solve();
        //        System.out.println("ColGen: " + solution);
        //        System.out.println(solution.getLowerBound());
        //        System.out.println(columnGeneration.getNumberOfIterations());

        BranchAndPrice branchAndPrice =
                new BranchAndPrice(instance, new GeRestrictedMasterProblem(instance), new LabelSettingPricing(instance),
                        new InitialSolutionHeuristic(instance));
        Solution solution = branchAndPrice.solve();
        System.out.println("B&P: " + solution);
        System.out.println(solution.getLowerBound());

        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        solution = columnGeneration.solve();
        System.out.println("ColGen: " + solution);
        System.out.println(solution.getLowerBound());
        System.out.println(columnGeneration.getNumberOfIterations());

    }
}
