import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.List;

public interface RestrictedMasterProblem {

    void addPaths(List<ElementaryPath> newPaths);

    Solution solveRelaxation();

    IntegerSolution solveInteger();

    List<ElementaryPath> computePathsFromSolution() throws IloException;

    class Solution {
        private final double objectiveValue;
        private final double[] customerDuals;
        private final double vehiclesDual;

        public Solution(IloCplex cplex, IloRange[] customerServedConstraints, IloRange numberOfVehiclesConstraint)
                throws IloException {
            if (!Utils.isSolutionFeasible(cplex)) {
                throw new IllegalStateException("Restricted Master Problem is not feasible");
            }
            this.objectiveValue = cplex.getObjValue();
            this.customerDuals = cplex.getDuals(customerServedConstraints);
            this.vehiclesDual = cplex.getDual(numberOfVehiclesConstraint);
        }

        public double getVehiclesDual() {
            return vehiclesDual;
        }

        public double getCustomerDual(int constraintIndex) {
            return customerDuals[constraintIndex];
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }

    class IntegerSolution {

        private final double objectiveValue;
        private final List<ElementaryPath> usedPaths;

        public IntegerSolution(IloCplex cplex, RestrictedMasterProblem rmp) throws IloException {
            this.objectiveValue = cplex.getObjValue();
            this.usedPaths = rmp.computePathsFromSolution();
        }

        public List<ElementaryPath> getUsedPaths() {
            return usedPaths;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }
}
