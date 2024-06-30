package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.Branch;
import algorithm.branching.BranchOnFleetSize;
import algorithm.branching.BranchOnVisitFlow;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class PricingProblem {

  private final Deque<Branch> activeBranches;

  public PricingProblem() {
    this.activeBranches = new ArrayDeque<>();
  }

  public abstract PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime);

  public abstract void forceExactSolution();

  public abstract void performBranchOnVisitFlow(BranchOnVisitFlow branch);

  public abstract void performBranchOnFleetSize(BranchOnFleetSize branch);

  public Deque<Branch> getActiveBranches() {
    return activeBranches;
  }

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
      } else if (branch instanceof BranchOnFleetSize) {
        performBranchOnFleetSize((BranchOnFleetSize) branch);
      }
    }
  }
}
