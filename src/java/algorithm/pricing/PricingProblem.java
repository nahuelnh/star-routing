package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchingDirection;
import commons.FeasiblePath;

import java.time.Duration;
import java.util.List;

public interface PricingProblem {

    PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    void forceExactSolution();

    void addBranch(BranchingDirection branch);

    void removeBranch(BranchingDirection branch);

}
