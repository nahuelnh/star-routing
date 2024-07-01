package main;

import algorithm.ColumnGenerator;
import algorithm.GeRestrictedMasterProblem;
import algorithm.InitialSolutionHeuristic;
import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PulsePricing;
import commons.Instance;
import commons.InstanceLoader;
import commons.StarRoutingSolution;
import java.time.Duration;

public class Main {

  private static final Duration TIMEOUT = Duration.ofMinutes(1);

  public static void main(String[] args) {
    for (Instance instance : InstanceLoader.getInstance().getExperimentInstances()) {
      if (instance.getNumberOfNodes() >= 40) {
        runInstance(instance.getName());
      }
    }
  }

  private static void runInstance(String instanceName) {
    System.out.println("Running instance: " + instanceName);
    Instance instance = new Instance(instanceName, true);
    /*
            CompactModel compactModel = new CompactModel(instance);
            System.out.println("MTZ: " + compactModel.solve());
             columnGenerator = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            columnGenerator.finishEarly(0.05);
             solution = columnGenerator.solve();
            System.out.println("ColGen: " + solution);
            System.out.println(solution.getLowerBound());
            System.out.println(columnGenerator.getNumberOfIterations());

            columnGenerator = new ColumnGenerator(instance, new GeRestrictedMasterProblem(instance),
                    new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
            columnGenerator.applyRearrangeCustomersHeuristic();
            solution = columnGenerator.solve();
            System.out.println("ColGen: " + solution);
            System.out.println(solution.getLowerBound());
            System.out.println(columnGenerator.getNumberOfIterations());
    */

    //        BranchAndPrice branchAndPrice = new BranchAndPrice(instance,
    //                                                           new
    // GeRestrictedMasterProblem(instance),
    //                                                           new LabelSettingPricing(instance),
    //                                                           new
    // InitialSolutionHeuristic(instance));
    //        StarRoutingSolution solution1 = branchAndPrice.solve(TIMEOUT);
    //        System.out.println("B&P: " + solution);
    //        System.out.println(solution.getLowerBound());

    ColumnGenerator branchAndPrice =
        new ColumnGenerator(
            instance,
            new GeRestrictedMasterProblem(instance),
            new PulsePricing(instance),
            new InitialSolutionHeuristic(instance));
    StarRoutingSolution solution1 = branchAndPrice.solve(TIMEOUT);

    ColumnGenerator columnGenerator =
        new ColumnGenerator(
            instance,
            new GeRestrictedMasterProblem(instance),
            new LabelSettingPricing(instance),
            new InitialSolutionHeuristic(instance));
    StarRoutingSolution solution2 = columnGenerator.solve(TIMEOUT);

    System.out.println("B&P: " + solution1);
    if (solution1.hasLowerBound()) {
      System.out.println(solution1.getLowerBound());
    }
    System.out.println("ColGen: " + solution2);
    if (solution2.hasLowerBound()) {
      System.out.println(solution2.getLowerBound());
    }
    System.out.println();

    //        System.out.println("ColGen: " + solution);
    //        System.out.println(solution.getLowerBound());
    //        System.out.println(columnGenerator.getNumberOfIterations());

  }
}
