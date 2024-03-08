package algorithm;

import algorithm.pricing.PricingProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;
import commons.Stopwatch;
import commons.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class BranchAndPrice {

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

    public Solution solve() {
        Stopwatch stopwatch = new Stopwatch(Utils.DEFAULT_TIMEOUT);
        List<FeasiblePath> columnsToAdd = initialSolutionHeuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        RestrictedMasterProblem.RMPSolution rmpSolution = new RestrictedMasterProblem.RMPSolution();
        RestrictedMasterProblem.RMPSolution incumbent = new RestrictedMasterProblem.RMPSolution();

        ArrayDeque<Node> remainingNodes = new ArrayDeque<>();
        remainingNodes.add(new Node(null, null));

        Node lastNode;
        Node currentNode = null;
        while (!remainingNodes.isEmpty()) {
            lastNode = currentNode;
            currentNode = remainingNodes.getFirst();
            remainingNodes.removeFirst();

            updateSubproblems(lastNode, currentNode);

            while (true) {
                rmp.addPaths(columnsToAdd);
                rmpSolution = rmp.solveRelaxation(stopwatch.getRemainingTime());
                if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
                    break;
                }
                relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());

                PricingProblem.PricingSolution pricingSolution =
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
                if (rmpSolution.isInteger()) {
                    upperBound = rmpSolution.getObjectiveValue();
                    incumbent = rmpSolution;
                } else {
                    RestrictedMasterProblem.RMPIntegerSolution rmpSolution2 =
                            rmp.solveInteger(stopwatch.getRemainingTime());
                    if (rmpSolution2.getObjectiveValue() < upperBound) {
                        upperBound = rmpSolution2.getObjectiveValue();
                        incumbent = rmpSolution;
                    }
                    for (BranchingDirection branch : branchingRuleManager.getBranches(rmpSolution)) {
                        remainingNodes.addFirst(new Node(currentNode, branch));
                    }
                }
            }
        }

        return buildSolution(stopwatch, relaxationOptimal, incumbent, 0, true);
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

    private Solution buildSolution(Stopwatch stopwatch, double relaxationOptimal,
                                   RestrictedMasterProblem.RMPSolution rmpSolution, double deterministicTime,
                                   boolean integral) {
        Solution solution;
        if (stopwatch.timedOut()) {
            if (relaxationOptimal == Double.MAX_VALUE) {
                solution = new Solution(Solution.Status.UNKNOWN, relaxationOptimal, stopwatch.getElapsedTime(), false);

            } else {
                solution = new Solution(Solution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime(),
                        rmpSolution.isInteger());
            }
        } else if (integral) {
            RestrictedMasterProblem.RMPIntegerSolution rmpIntegerSolution =
                    rmp.solveInteger(stopwatch.getRemainingTime());
            if (!rmpIntegerSolution.isFeasible()) {
                solution =
                        new Solution(Solution.Status.INFEASIBLE, relaxationOptimal, stopwatch.getElapsedTime(), false);
            } else if (stopwatch.timedOut()) {
                solution = new Solution(Solution.Status.TIMEOUT, relaxationOptimal, stopwatch.getElapsedTime(), false);
            } else {
                solution = new Solution(Solution.Status.OPTIMAL, rmpIntegerSolution.getObjectiveValue(),
                        rmpIntegerSolution.getUsedPaths(), stopwatch.getElapsedTime());
                solution.setLowerBound(relaxationOptimal);
            }
        } else {
            solution = new Solution(Solution.Status.FEASIBLE, relaxationOptimal, stopwatch.getElapsedTime(), false);
        }
        solution.setDeterministicTime(deterministicTime);
        return solution;
    }

    public static class Node {
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
