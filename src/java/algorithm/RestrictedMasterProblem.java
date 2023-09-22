package algorithm;

import commons.FeasiblePath;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface RestrictedMasterProblem {

    void addPaths(List<FeasiblePath> newPaths);

    RMPSolution solveRelaxation(Duration remainingTime);

    RMPIntegerSolution solveInteger(Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    class RMPSolution {
        private final double objectiveValue;
        private final double[] customerDuals;
        private final double vehiclesDual;
        private final boolean feasible;

        public RMPSolution(double objectiveValue, double[] customerDuals, double vehiclesDual, boolean feasible) {
            this.objectiveValue = objectiveValue;
            this.customerDuals = customerDuals;
            this.vehiclesDual = vehiclesDual;
            this.feasible = feasible;
        }

        public RMPSolution() {
            this.objectiveValue = 0.0;
            this.customerDuals = new double[]{};
            this.vehiclesDual = 0.0;
            this.feasible = false;
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

        public boolean isFeasible() {
            return feasible;
        }
    }

    class RMPIntegerSolution {

        private final double objectiveValue;
        private final List<FeasiblePath> usedPaths;
        private final boolean feasible;

        public RMPIntegerSolution(double objectiveValue, List<FeasiblePath> usedPaths, boolean feasible) {
            this.objectiveValue = objectiveValue;
            this.usedPaths = usedPaths;
            this.feasible = feasible;
        }

        public RMPIntegerSolution() {
            this.objectiveValue = 0.0;
            this.usedPaths = new ArrayList<>();
            this.feasible = false;
        }

        public List<FeasiblePath> getUsedPaths() {
            return usedPaths;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }

        public boolean isFeasible() {
            return feasible;
        }
    }
}
