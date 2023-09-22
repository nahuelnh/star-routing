package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface PricingProblem {

    PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    class PricingSolution {
        private final boolean feasible;
        private final double objectiveValue;
        private final List<FeasiblePath> negativeReducedCostPaths;

        public PricingSolution(double objectiveValue, List<FeasiblePath> negativeReducedCostPaths, boolean feasible) {
            this.feasible = feasible;
            this.objectiveValue = objectiveValue;
            this.negativeReducedCostPaths = negativeReducedCostPaths;
        }

        public PricingSolution() {
            this.feasible = false;
            this.objectiveValue = 0.0;
            this.negativeReducedCostPaths = new ArrayList<>();
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
    }
}
