package algorithm;

import commons.FeasiblePath;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface RestrictedMasterProblem {

    void addPaths(List<FeasiblePath> newPaths);

    RMPSolution solveRelaxation(Duration remainingTime);

    RMPIntegerSolution solveInteger(Duration remainingTime);

    List<FeasiblePath> computePathsFromSolution();

    void addBranch(BranchingDirection branch);

    void removeBranch(BranchingDirection branch);

    double fluxOnEdge(int start, int end);

    class RMPSolution {
        private final double objectiveValue;
        private final double[] customerDuals;
        private final double vehiclesDual;
        private final List<Double> fluxDuals;
        private final boolean feasible;
        private final double[] primalValues;
        private final boolean isInteger;
        private final Map<Integer, Map<Integer, Double>> flux;

        public RMPSolution(double objectiveValue, double[] customerDuals, double vehiclesDual, List<Double> fluxDuals,
                           double[] primalValues, boolean feasible, boolean isInteger,
                           Map<Integer, Map<Integer, Double>> flux) {
            this.objectiveValue = objectiveValue;
            this.customerDuals = customerDuals;
            this.vehiclesDual = vehiclesDual;
            this.fluxDuals = fluxDuals;
            this.primalValues = primalValues;
            this.feasible = feasible;
            this.isInteger = isInteger;
            this.flux = flux;
        }

        public RMPSolution() {
            this.objectiveValue = 0.0;
            this.customerDuals = new double[]{};
            this.primalValues = new double[]{};
            this.vehiclesDual = 0.0;
            this.fluxDuals = new ArrayList<>();
            this.feasible = false;
            this.isInteger = false;
            this.flux = new HashMap<>();
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

        public double getPrimalValue(int route) {
            return primalValues[route];
        }

        public boolean isInteger() {
            return isInteger;
        }

        public double getFlux(int i, int j) {
            return flux.get(i).get(j);
        }

        public List<Double> getFluxDuals() {
            return fluxDuals;
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
