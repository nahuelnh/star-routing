package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import ilog.cplex.IloCplex;

import java.util.List;

public interface PricingProblem {

    PricingProblem.Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution);

    List<FeasiblePath> computePathsFromSolution();

    class Solution {
        private final IloCplex.Status status;
        private final double objectiveValue;
        private final List<FeasiblePath> negativeReducedCostPaths;

        Solution(IloCplex.Status status, double objectiveValue, PricingProblem pricingProblem) {
            this.status = status;
            this.objectiveValue = objectiveValue;
            this.negativeReducedCostPaths = pricingProblem.computePathsFromSolution();
        }

        public List<FeasiblePath> getNegativeReducedCostPaths() {
            return negativeReducedCostPaths;
        }

        public IloCplex.Status getStatus() {
            return status;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }
}
