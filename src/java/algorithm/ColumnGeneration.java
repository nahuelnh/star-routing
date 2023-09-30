package algorithm;

import algorithm.pricing.PricingProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ColumnGeneration {

    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final InitialSolutionHeuristic initialSolutionHeuristic;
    private final RearrangeCustomersHeuristic rearrangeCustomersHeuristic;
    private boolean applyRearrangeCustomersHeuristic;
    private int numberOfIterations;

    public ColumnGeneration(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem,
                            InitialSolutionHeuristic initialSolutionHeuristic) {
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.initialSolutionHeuristic = initialSolutionHeuristic;
        this.rearrangeCustomersHeuristic = new RearrangeCustomersHeuristic(instance);
        this.applyRearrangeCustomersHeuristic = false;
        this.numberOfIterations = 0;
    }

    private Solution buildSolution(Stopwatch stopwatch, double relaxationOptimal, double deterministicTime,
                                   boolean integral) {
        Solution solution;
        if (stopwatch.timedOut()) {
            solution = new Solution(Solution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime());
        } else if (integral) {
            RestrictedMasterProblem.RMPIntegerSolution rmpSolution = rmp.solveInteger(stopwatch.getRemainingTime());
            if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
                solution = new Solution(Solution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime());
            } else {
                solution = new Solution(Solution.Status.OPTIMAL, rmpSolution.getObjectiveValue(),
                        rmpSolution.getUsedPaths(), stopwatch.getElapsedTime());
                solution.setLowerBound(relaxationOptimal);
            }
        } else {
            solution = new Solution(Solution.Status.FEASIBLE, relaxationOptimal, stopwatch.getElapsedTime());
        }
        solution.setDeterministicTime(deterministicTime);
        return solution;
    }

    private Solution generateColumns(boolean integral, Duration timeout) {
        Stopwatch stopwatch = new Stopwatch(timeout);
        List<FeasiblePath> columnsToAdd = initialSolutionHeuristic.run();
        List<FeasiblePath> allColumns = new ArrayList<>();
        double relaxationOptimal = Double.MAX_VALUE;
        double deterministicTime = 0.0;
        while (true) {
            numberOfIterations++;
            allColumns.addAll(columnsToAdd);
            rmp.addPaths(columnsToAdd);
            RestrictedMasterProblem.RMPSolution rmpSolution = rmp.solveRelaxation(stopwatch.getRemainingTime());
            if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
                break;
            }
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            PricingProblem.PricingSolution pricingSolution = pricing.solve(rmpSolution, stopwatch.getRemainingTime());
            if (!pricingSolution.isFeasible() || stopwatch.timedOut()) {
                break;
            }
            deterministicTime += pricingSolution.getDeterministicTime();
            columnsToAdd = pricingSolution.getNegativeReducedCostPaths();
            if (columnsToAdd.isEmpty()) {
                break;
            }
            if (applyRearrangeCustomersHeuristic) {
                columnsToAdd.addAll(rearrangeCustomersHeuristic.run(allColumns, rmpSolution));
            }
        }
        return buildSolution(stopwatch, relaxationOptimal, deterministicTime, integral);
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

    public void applyRearrangeCustomersHeuristic() {
        this.applyRearrangeCustomersHeuristic = true;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }
}

