package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class TwoStepColumnGeneration {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final FirstStepPricingProblem firstPricing;
    private final PricingProblem secondPricing;
    private final FeasibleSolutionHeuristic heuristic;

    public TwoStepColumnGeneration(Instance instance, RestrictedMasterProblem rmp, FirstStepPricingProblem firstPricingProblem,
                                   PricingProblem secondPricingProblem, FeasibleSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.firstPricing = firstPricingProblem;
        this.secondPricing = secondPricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance instance = new Instance("instance_neighbors_40", true);
        TwoStepColumnGeneration columnGeneration =
                new TwoStepColumnGeneration(instance, new GeRestrictedMasterProblem(instance),
                        new FirstStepPricingProblem(instance), new LabelSettingPricing(instance),
                        new FeasibleSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        Instant start = Instant.now();
        List<FeasiblePath> newPaths = heuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        while (!newPaths.isEmpty()) {
            rmp.addPaths(newPaths);
            firstPricing.addPaths(newPaths);
            RestrictedMasterProblem.RMPSolution rmpSolution = rmp.solveRelaxation();
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            PricingProblem.Solution pricingSolution = firstPricing.solve(rmpSolution);
            newPaths = pricingSolution.getNegativeReducedCostPaths();

//            if (newPaths.isEmpty()) {
//                break;
//            }

            rmp.addPaths(newPaths);
            firstPricing.addPaths(newPaths);
            rmpSolution = rmp.solveRelaxation();
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            pricingSolution = secondPricing.solve(rmpSolution);
            newPaths = pricingSolution.getNegativeReducedCostPaths();
        }
        System.out.println("Relaxation optimal:" + relaxationOptimal);
        RestrictedMasterProblem.RMPIntegerSolution solution = rmp.solveInteger();
        System.out.println("Integer optimal:" + solution.getObjectiveValue());
        Instant finish = Instant.now();
        return new Solution(solution.getUsedPaths(), Duration.between(start, finish));
    }
}

