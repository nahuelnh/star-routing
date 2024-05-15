package algorithm.branching;

import algorithm.InitialSolutionHeuristic;
import algorithm.RMPIntegerSolution;
import algorithm.RMPLinearSolution;
import algorithm.RestrictedMasterProblem;
import algorithm.pricing.PricingProblem;
import algorithm.pricing.PricingSolution;
import commons.FeasiblePath;
import commons.Instance;
import commons.StarRoutingSolution;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class BranchAndPrice {

    private static final Double EPSILON = 0.01;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final InitialSolutionHeuristic initialSolutionHeuristic;
    private final BranchingRuleManager branchingRuleManager;
    private double upperBound;

    public BranchAndPrice(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricing,
                          InitialSolutionHeuristic initialSolutionHeuristic) {
        this.rmp = rmp;
        this.pricing = pricing;
        this.initialSolutionHeuristic = initialSolutionHeuristic;
        this.branchingRuleManager = new BranchingRuleManager(instance);
        this.upperBound = Double.MAX_VALUE;
    }

    public StarRoutingSolution solve() {
        return solve(Utils.DEFAULT_TIMEOUT);
    }

    public StarRoutingSolution solve(Duration timeout) {
        Stopwatch stopwatch = new Stopwatch(timeout);
        List<FeasiblePath> columnsToAdd = initialSolutionHeuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        this.upperBound = Double.MAX_VALUE;
        RMPLinearSolution rmpSolution = null;
        RMPIntegerSolution incumbent = null;

        ArrayDeque<Node> remainingNodes = new ArrayDeque<>();
        remainingNodes.add(new Node(null, null));

        Node lastNode;
        Node currentNode = null;
        while (!remainingNodes.isEmpty()) {
            lastNode = currentNode;
            currentNode = remainingNodes.removeFirst();

            updateSubproblems(lastNode, currentNode);

            while (true) {
                rmp.addColumns(columnsToAdd);
                rmp.solveRelaxation(stopwatch.getRemainingTime());
                rmpSolution = rmp.getSolution();
                if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
                    break;
                }
                relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());

                PricingSolution pricingSolution =
                        pricing.solve(rmpSolution, stopwatch.getRemainingTime());
                if (!pricingSolution.isFeasible() || stopwatch.timedOut()) {
                    break;
                }
                columnsToAdd = pricingSolution.getNegativeReducedCostPaths();
                if (columnsToAdd.isEmpty()) {
                    break;
                }
            }
            if (rmpSolution.isFeasible() && Math.ceil(rmpSolution.getObjectiveValue()) < upperBound) {
                rmp.solveInteger(stopwatch.getRemainingTime());
                RMPIntegerSolution rmpIntegerSolution = rmp.getIntegerSolution();
                if (rmpSolution.isInteger()) {
                    upperBound = rmpSolution.getObjectiveValue();
                    incumbent = rmpIntegerSolution;
                    assert Math.abs(rmpIntegerSolution.getObjectiveValue() - rmpSolution.getObjectiveValue()) < EPSILON;
                } else {
                    if (rmpIntegerSolution.getObjectiveValue() < upperBound) {
                        upperBound = rmpIntegerSolution.getObjectiveValue();
                        incumbent = rmpIntegerSolution;
                    }
                    for (BranchingDirection branch : branchingRuleManager.getBranches(rmpSolution)) {
                        remainingNodes.addFirst(new Node(currentNode, branch));
                    }
                }
            }
        }
        return buildSolution(stopwatch, relaxationOptimal, incumbent);
    }

    private void updateSubproblems(Node last, Node current) {
        if (last == null) {
            return;
        }
        List<Node> fromLast = last.pathToRoot();
        List<Node> fromCurrent = current.pathToRoot();
        int i = 0;
        while (!fromCurrent.contains(fromLast.get(i))) {
            rmp.removeBranch(fromLast.get(i).getBranch());
            pricing.removeBranch(fromLast.get(i).getBranch());
            ++i;
        }
        int j = fromCurrent.indexOf(fromLast.get(i)) - 1;
        while (j >= 0) {
            rmp.addBranch(fromCurrent.get(j).getBranch());
            pricing.addBranch(fromCurrent.get(j).getBranch());
            --j;
        }
    }

    private StarRoutingSolution buildSolution(Stopwatch stopwatch, double relaxationOptimal,
                                              RMPIntegerSolution incumbent) {
        if (stopwatch.timedOut() && relaxationOptimal == Double.MAX_VALUE) {
            return new StarRoutingSolution(StarRoutingSolution.Status.UNKNOWN, relaxationOptimal,
                    stopwatch.getElapsedTime(), false);
        }
        if (incumbent == null) {
            return new StarRoutingSolution(StarRoutingSolution.Status.UNKNOWN, relaxationOptimal,
                    stopwatch.getElapsedTime(), false);
        } else if (stopwatch.timedOut()) {
            StarRoutingSolution solution =
                    new StarRoutingSolution(StarRoutingSolution.Status.TIMEOUT, incumbent.getObjectiveValue(),
                            incumbent.getUsedPaths(), stopwatch.getElapsedTime());
            solution.setLowerBound(relaxationOptimal);
            return solution;
        } else if (!incumbent.isFeasible()) {
            return new StarRoutingSolution(StarRoutingSolution.Status.INFEASIBLE, relaxationOptimal,
                    stopwatch.getElapsedTime(), false);
        } else {
            StarRoutingSolution solution =
                    new StarRoutingSolution(StarRoutingSolution.Status.OPTIMAL, incumbent.getObjectiveValue(),
                            incumbent.getUsedPaths(), stopwatch.getElapsedTime());
            solution.setLowerBound(relaxationOptimal);
            return solution;
        }
    }

    private static class Node {
        private final Node parent;
        private final BranchingDirection branch;

        public Node(Node parent, BranchingDirection branch) {
            this.parent = parent;
            this.branch = branch;
        }

        public Node getParent() {
            return parent;
        }

        public List<Node> pathToRoot() {
            List<Node> ret = new ArrayList<>();
            Node current = this;
            while (current != null) {
                ret.add(current);
                current = current.parent;
            }
            return ret;
        }

        public BranchingDirection getBranch() {
            return branch;
        }
    }

}
