package algorithm.pricing;

import commons.Route;

import java.util.ArrayList;
import java.util.List;

public class PricingSolution {

  private final boolean feasible;
  private final double objectiveValue;
  private final List<Route> negativeReducedCostPaths;
  private final double deterministicTime;

  public PricingSolution(
      double objectiveValue,
      List<Route> negativeReducedCostPaths,
      double deterministicTime,
      boolean feasible) {
    this.feasible = feasible;
    this.objectiveValue = objectiveValue;
    this.negativeReducedCostPaths = negativeReducedCostPaths;
    this.deterministicTime = deterministicTime;
  }

  public PricingSolution() {
    this(0.0, new ArrayList<>(), 0.0, false);
  }

  public List<Route> getNegativeReducedCostPaths() {
    return negativeReducedCostPaths;
  }

  public boolean isFeasible() {
    return feasible;
  }

  public double getObjectiveValue() {
    return objectiveValue;
  }

  public double getDeterministicTime() {
    return deterministicTime;
  }
}
