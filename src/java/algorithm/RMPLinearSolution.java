package algorithm;

import algorithm.branching.BranchOnEdge;

import java.util.HashMap;
import java.util.Map;

public class RMPLinearSolution {
    private final double objectiveValue;
    private final double[] customerDuals;
    private final double vehiclesDual;
    private final Map<BranchOnEdge, Double> flowDuals;
    private final boolean feasible;
    private final double[] primalValues;
    private final boolean isInteger;
    private final Map<Integer, Map<Integer, Double>> flow;

    public RMPLinearSolution(double objectiveValue, double[] customerDuals, double vehiclesDual,
                             Map<BranchOnEdge, Double> flowDuals, double[] primalValues, boolean feasible,
                             boolean isInteger, Map<Integer, Map<Integer, Double>> flow) {
        this.objectiveValue = objectiveValue;
        this.customerDuals = customerDuals;
        this.vehiclesDual = vehiclesDual;
        this.flowDuals = flowDuals;
        this.primalValues = primalValues;
        this.feasible = feasible;
        this.isInteger = isInteger;
        this.flow = flow;
    }

    public RMPLinearSolution() {
        this.objectiveValue = Double.MAX_VALUE;
        this.customerDuals = new double[]{};
        this.primalValues = new double[]{};
        this.vehiclesDual = 0.0;
        this.flowDuals = new HashMap<>();
        this.feasible = false;
        this.isInteger = false;
        this.flow = new HashMap<>();
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

    public double getFlow(int i, int j) {
        return flow.get(i).get(j);
    }

    public boolean hasFlowDual(BranchOnEdge branch) {
        return flowDuals.containsKey(branch);
    }

    public double getFlowDual(BranchOnEdge branch) {
        return flowDuals.get(branch);
    }
}
