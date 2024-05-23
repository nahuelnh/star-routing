package algorithm;

import algorithm.branching.BranchOnVisitFlow;
import commons.VisitFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RMPLinearSolution {

    private final double objectiveValue;
    private final double[] customerDuals;
    private final double vehiclesDual;
    private final boolean feasible;
    private final double[] primalValues;
    private final double numberOfVehicles;
    private final List<Double> fleetSizeDuals;
    private final boolean isInteger;
    private final List<VisitFlow> visitFlow;
    private final Map<BranchOnVisitFlow, Double> visitFlowDuals;

    public RMPLinearSolution(double objectiveValue, double[] customerDuals, double vehiclesDual, boolean feasible,
                             double[] primalValues, double numberOfVehicles, List<Double> fleetSizeDuals, boolean isInteger, List<VisitFlow> visitFlow,
                             Map<BranchOnVisitFlow, Double> visitFlowDuals) {
        this.objectiveValue = objectiveValue;
        this.customerDuals = customerDuals;
        this.vehiclesDual = vehiclesDual;
        this.feasible = feasible;
        this.primalValues = primalValues;
        this.numberOfVehicles = numberOfVehicles;
        this.fleetSizeDuals = fleetSizeDuals;
        this.isInteger = isInteger;
        this.visitFlow = visitFlow;
        this.visitFlowDuals = visitFlowDuals;
    }

    public RMPLinearSolution() {
        this.objectiveValue = Double.MAX_VALUE;
        this.customerDuals = new double[]{};
        this.primalValues = new double[]{};
        this.numberOfVehicles = 0.0;
        this.fleetSizeDuals = new ArrayList<>();
        this.vehiclesDual = 0.0;
        this.feasible = false;
        this.isInteger = false;
        this.visitFlow = new ArrayList<>();
        this.visitFlowDuals = new HashMap<>();
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

    public List<VisitFlow> getVisitFlow() {
        return visitFlow;
    }

    public Map<BranchOnVisitFlow, Double> getVisitFlowDuals() {
        return visitFlowDuals;
    }

    public double getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public List<Double> getFleetSizeDuals() {
        return fleetSizeDuals;
    }
}
