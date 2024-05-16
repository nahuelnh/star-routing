package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.Branch;
import commons.FeasiblePath;

import java.time.Duration;
import java.util.List;

public interface PricingProblem {

    PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    void forceExactSolution();

    void addBranch(Branch branch);

    void removeBranch(Branch branch);

}
