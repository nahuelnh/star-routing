package algorithm.pricing;

import algorithm.BranchingDirection;
import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface PricingProblem {

    PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    void forceExactSolution();

    void addBranch(BranchingDirection branch);

    void removeBranch(BranchingDirection branch);

    class PricingSolution {
        private final boolean feasible;
        private final double objectiveValue;
        private final List<FeasiblePath> negativeReducedCostPaths;
        private final double deterministicTime;

        public PricingSolution(double objectiveValue, List<FeasiblePath> negativeReducedCostPaths,
                               double deterministicTime, boolean feasible) {
            this.feasible = feasible;
            this.objectiveValue = objectiveValue;
            this.negativeReducedCostPaths = negativeReducedCostPaths;
            this.deterministicTime = deterministicTime;
        }

        public PricingSolution() {
            this(0.0, new ArrayList<>(), 0.0, false);
        }

        public List<FeasiblePath> getNegativeReducedCostPaths() {
            return negativeReducedCostPaths;
        }

        public boolean isFeasible() {
            return feasible;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }

        public double getDeterministicTime() {
            return deterministicTime;
        }
    }
}
