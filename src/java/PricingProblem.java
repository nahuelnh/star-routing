import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.List;

public interface PricingProblem {

    PricingProblem.Solution solve(RestrictedMasterProblem.Solution rmpSolution);

    List<ElementaryPath> computePathsFromSolution() throws IloException;

    class Solution {
        private final IloCplex.Status status;
        private final double objectiveValue;
        private final List<ElementaryPath> negativeReducedCostPaths;

        Solution(IloCplex cplex, PricingProblem pricingProblem) throws IloException {
            this.status = cplex.getStatus();
            this.objectiveValue = cplex.getObjValue();
            this.negativeReducedCostPaths = pricingProblem.computePathsFromSolution();
        }

        public List<ElementaryPath> getNegativeReducedCostPaths() {
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
