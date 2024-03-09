package main;

import algorithm.ColumnGeneration;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.Solution;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_n16_s13_k4");
    }

    private static void runInstance(String instanceName) {
        System.out.println("Running instance: " + instanceName);
        Instance instance = new Instance(instanceName, true);
        //        CompactModel compactModel = new CompactModel(instance);
        //        System.out.println("MTZ: " + compactModel.solve());
//        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
//                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
//        Solution solution = columnGeneration.solve();
//        System.out.println("ColGen: " + solution);
//        System.out.println(solution.getLowerBound());
//        System.out.println(columnGeneration.getNumberOfIterations());


        ColumnGeneration columnGeneration =
                new ColumnGeneration(instance, new GeRestrictedMasterProblem(instance), new PulsePricing(instance),
                        new InitialSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
                System.out.println("ColGen: " + solution);
                System.out.println(solution.getLowerBound());
                System.out.println(columnGeneration.getNumberOfIterations());



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
    }
}
