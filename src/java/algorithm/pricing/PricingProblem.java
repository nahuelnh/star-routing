package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.Branch;
import algorithm.branching.BranchOnVisitFlow;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class PricingProblem {

    private final Deque<Branch> activeBranches;

    public PricingProblem() {
        this.activeBranches = new ArrayDeque<>();
    }

    public Deque<Branch> getActiveBranches() {
        return activeBranches;
    }

    public abstract PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime);

    public abstract void forceExactSolution();

    public void addBranch(Branch branch) {
        activeBranches.add(branch);
    }

    public void removeBranch(Branch branch) {
        activeBranches.removeLast();
    }

    void performBranching() {
        for (Branch branch : activeBranches) {
            if (branch instanceof BranchOnVisitFlow) {
                performBranchOnVisitFlow((BranchOnVisitFlow) branch);
            }
        }
    }

    private void performBranchOnVisitFlow(BranchOnVisitFlow branch) {
        //        if (branch.isUpperBound() && branch.getBound() == 0) {
        //            graph.removeEdge(branch.getStart(), graph.translateToESPPRCNode(branch.getEnd()));
        //        }
        //        if (branch.isLowerBound() && branch.getBound() >= 1) {
        //            for (int node : graph.getAdjacentNodes(branch.getStart())) {
        //                if (branch.getEnd() == instance.getDepot() && node != graph.getEnd()) {
        //                    graph.removeEdge(graph.getStart(), graph.translateToESPPRCNode(node));
        //                }
        //                if (branch.getEnd() != instance.getDepot() && node != branch.getEnd()) {
        //                    graph.removeEdge(graph.getStart(), graph.translateToESPPRCNode(node));
        //                }
        //            }
        //        }
    }

}
