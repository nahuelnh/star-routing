package algorithm;

import algorithm.pricing.LabelSettingPricing;
import algorithm.pricing.PricingProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;
import commons.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ColumnGeneration {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final InitialSolutionHeuristic heuristic;

    public ColumnGeneration(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem,
                            InitialSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance instance = new Instance("instance_neighbors_40", true);
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new EqRestrictedMasterProblem(instance),
                new LabelSettingPricing(instance), new InitialSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }



    private Solution generateColumns(boolean integral, Duration timeout) {
        Instant start = Instant.now();
        List<FeasiblePath> newPaths = heuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        while (!newPaths.isEmpty()) {
            rmp.addPaths(newPaths);
            RestrictedMasterProblem.RMPSolution rmpSolution = rmp.solveRelaxation(Utils.getRemainingTime(start, timeout));
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            PricingProblem.PricingSolution pricingSolution =
                    pricing.solve(rmpSolution, Utils.getRemainingTime(start, timeout));
            newPaths = pricingSolution.getNegativeReducedCostPaths();
        }
        if (integral) {
            RestrictedMasterProblem.RMPIntegerSolution solution = rmp.solveInteger(Utils.getRemainingTime(start, timeout));
            return new Solution(Solution.Status.FINISHED, solution.getObjectiveValue(), solution.getUsedPaths(),
                    Utils.getElapsedTime(start));
        } else {
            return new Solution(Solution.Status.FINISHED, relaxationOptimal, new ArrayList<>(), Utils.getElapsedTime(start));
        }
    }

    public Solution solve(Duration timeout) {
        return generateColumns(true, timeout);
    }

    public Solution solve() {
        return solve(Utils.DEFAULT_TIMEOUT);
    }

    public Solution solveRelaxation(Duration timeout) {
        return generateColumns(false, timeout);
    }

    public Solution solveRelaxation() {
        return solveRelaxation(Utils.DEFAULT_TIMEOUT);
    }

}

