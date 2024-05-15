package algorithm;

import algorithm.pricing.PricingProblem;
import algorithm.pricing.PricingSolution;
import commons.FeasiblePath;
import commons.Instance;
import commons.StarRoutingSolution;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ColumnGenerator {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final InitialSolutionHeuristic initialSolutionHeuristic;
    private final RearrangeCustomersHeuristic rearrangeCustomersHeuristic;
    private boolean applyRearrangeCustomersHeuristic;
    private int numberOfIterations;
    private boolean finishEarly;
    private double gapThreshold;

    public ColumnGenerator(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem,
                           InitialSolutionHeuristic initialSolutionHeuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.initialSolutionHeuristic = initialSolutionHeuristic;
        this.rearrangeCustomersHeuristic = new RearrangeCustomersHeuristic(instance);
        this.applyRearrangeCustomersHeuristic = false;
        this.finishEarly = false;
        this.gapThreshold = 0;
        this.numberOfIterations = 0;
    }

    private StarRoutingSolution buildSolution(Stopwatch stopwatch, double relaxationOptimal, RMPLinearSolution rmpSolution,
                                              double deterministicTime, boolean integral) {
        StarRoutingSolution solution;
        if (stopwatch.timedOut()) {
            if (relaxationOptimal == Double.MAX_VALUE) {
                solution = new StarRoutingSolution(StarRoutingSolution.Status.UNKNOWN, relaxationOptimal, stopwatch.getElapsedTime(), false);
            } else {
                solution = new StarRoutingSolution(StarRoutingSolution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime(),
                        rmpSolution.isInteger());
            }
        } else if (integral) {
            rmp.solveInteger(stopwatch.getRemainingTime());
            RMPIntegerSolution rmpIntegerSolution = rmp.getIntegerSolution();
            if (!rmpIntegerSolution.isFeasible()) {
                solution =
                        new StarRoutingSolution(StarRoutingSolution.Status.INFEASIBLE, relaxationOptimal, stopwatch.getElapsedTime(), false);
            } else if (stopwatch.timedOut()) {
                solution = new StarRoutingSolution(StarRoutingSolution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime(), false);
            } else {
                solution = new StarRoutingSolution(StarRoutingSolution.Status.OPTIMAL, rmpIntegerSolution.getObjectiveValue(),
                        rmpIntegerSolution.getUsedPaths(), stopwatch.getElapsedTime());
                solution.setLowerBound(relaxationOptimal);
            }
        } else {
            solution = new StarRoutingSolution(StarRoutingSolution.Status.FEASIBLE, relaxationOptimal, stopwatch.getElapsedTime(), false);
        }
        solution.setDeterministicTime(deterministicTime);
        return solution;
    }

    private StarRoutingSolution generateColumns(boolean integral, Duration timeout) {
        Stopwatch stopwatch = new Stopwatch(timeout);
        List<FeasiblePath> columnsToAdd = initialSolutionHeuristic.run();
        List<FeasiblePath> allColumns = new ArrayList<>();
        double relaxationOptimal = Double.MAX_VALUE;
        double deterministicTime = 0.0;
        RMPLinearSolution rmpSolution;
        while (true) {
            numberOfIterations++;
            allColumns.addAll(columnsToAdd);
            rmp.addColumns(columnsToAdd);
            rmp.solveRelaxation(stopwatch.getRemainingTime());
            rmpSolution = rmp.getSolution();
            if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
                break;
            }
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            if (finishEarly) {
                pricing.forceExactSolution();
            }
            PricingSolution pricingSolution = pricing.solve(rmpSolution, stopwatch.getRemainingTime());
            if (!pricingSolution.isFeasible() || stopwatch.timedOut()) {
                break;
            }
            deterministicTime += pricingSolution.getDeterministicTime();
            columnsToAdd = pricingSolution.getNegativeReducedCostPaths();
            if (columnsToAdd.isEmpty()) {
                break;
            }
            if (finishEarly && computeGapToLowerBound(pricingSolution, relaxationOptimal) < gapThreshold) {
                break;
            }
            if (applyRearrangeCustomersHeuristic) {
                columnsToAdd.addAll(rearrangeCustomersHeuristic.run(allColumns, rmpSolution));
            }
        }
        return buildSolution(stopwatch, relaxationOptimal, rmpSolution, deterministicTime, integral);
    }

    private double computeGapToLowerBound(PricingSolution pricingSolution, double relaxationOptimal) {
        return Math.abs(instance.getNumberOfVehicles() * pricingSolution.getObjectiveValue() / relaxationOptimal);
    }

    public StarRoutingSolution solve(Duration timeout) {
        return generateColumns(true, timeout);
    }

    public StarRoutingSolution solve() {
        return solve(Utils.DEFAULT_TIMEOUT);
    }

    public StarRoutingSolution solveRelaxation(Duration timeout) {
        return generateColumns(false, timeout);
    }

    public StarRoutingSolution solveRelaxation() {
        return solveRelaxation(Utils.DEFAULT_TIMEOUT);
    }

    public void applyRearrangeCustomersHeuristic() {
        this.applyRearrangeCustomersHeuristic = true;
    }

    public void finishEarly(double gapThreshold) {
        this.finishEarly = true;
        this.gapThreshold = gapThreshold;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }
}

