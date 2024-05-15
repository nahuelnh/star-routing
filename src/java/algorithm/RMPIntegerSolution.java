package algorithm;

import commons.FeasiblePath;

import java.util.ArrayList;
import java.util.List;

public class RMPIntegerSolution {

    private final double objectiveValue;
    private final List<FeasiblePath> usedPaths;
    private final boolean feasible;

    public RMPIntegerSolution(double objectiveValue, List<FeasiblePath> usedPaths, boolean feasible) {
        this.objectiveValue = objectiveValue;
        this.usedPaths = usedPaths;
        this.feasible = feasible;
    }

    public RMPIntegerSolution() {
        this.objectiveValue = Double.MAX_VALUE;
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

    public boolean isInteger() {
        return true;
    }

}
