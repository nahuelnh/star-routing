package algorithm;

import commons.FeasiblePath;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.List;

public interface RestrictedMasterProblem {

    void addPaths(List<FeasiblePath> newPaths);

    RMPSolution solveRelaxation();

    RMPIntegerSolution solveInteger();

    List<FeasiblePath> computePathsFromSolution();

    class RMPSolution {
        private final double objectiveValue;
        private final double[] customerDuals;
        private final double vehiclesDual;

        public RMPSolution(IloCplex cplex, IloRange[] customerServedConstraints, IloRange numberOfVehiclesConstraint)
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

    class RMPIntegerSolution {

        private final double objectiveValue;
        private final List<FeasiblePath> usedPaths;

        public RMPIntegerSolution(IloCplex cplex, RestrictedMasterProblem rmp) throws IloException {
            this.objectiveValue = cplex.getObjValue();
            this.usedPaths = rmp.computePathsFromSolution();
        }

        public List<FeasiblePath> getUsedPaths() {
            return usedPaths;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }
}
