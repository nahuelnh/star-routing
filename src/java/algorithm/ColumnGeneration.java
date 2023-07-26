package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ColumnGeneration {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final FeasibleSolutionHeuristic heuristic;

    public ColumnGeneration(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem,
                            FeasibleSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance instance = new Instance("instance_neighbors_40", true);
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new EqRestrictedMasterProblem(instance),
                new SecondPricingProblem(instance), new FeasibleSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        Instant start = Instant.now();
        List<FeasiblePath> newPaths = heuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        while (!newPaths.isEmpty()) {
            rmp.addPaths(newPaths);
            RestrictedMasterProblem.RMPSolution rmpSolution = rmp.solveRelaxation();
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            PricingProblem.Solution pricingSolution = pricing.solve(rmpSolution);
            newPaths = pricingSolution.getNegativeReducedCostPaths();
        }
        System.out.println("Relaxation optimal:" + relaxationOptimal);
        RestrictedMasterProblem.RMPIntegerSolution solution = rmp.solveInteger();
        System.out.println("Integer optimal:" + solution.getObjectiveValue());
        Instant finish = Instant.now();
        return new Solution(solution.getUsedPaths(), Duration.between(start, finish));
    }
}

