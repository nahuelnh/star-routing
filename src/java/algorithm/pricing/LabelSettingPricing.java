package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnFleetSize;
import algorithm.branching.BranchOnVisitFlow;
import commons.Route;
import commons.Instance;
import commons.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelSettingPricing extends PricingProblem {

  private final Instance instance;
  private final boolean solveHeuristically;
  private final boolean isMono;
  private boolean forceExactSolution;
  private List<Route> paths;

  public LabelSettingPricing(Instance instance) {
    this(instance, false);
  }

  public LabelSettingPricing(Instance instance, boolean solveHeuristically) {
    this.instance = instance;
    this.paths = new ArrayList<>();
    this.solveHeuristically = solveHeuristically;
    this.forceExactSolution = false;
    this.isMono = false;
  }

  public LabelSettingPricing(Instance instance, boolean solveHeuristically, boolean isMono) {
    this.instance = instance;
    this.paths = new ArrayList<>();
    this.solveHeuristically = solveHeuristically;
    this.forceExactSolution = false;
    this.isMono = isMono;
  }

  private double getInitialCost(RMPLinearSolution rmpSolution) {
    double initialCost = -rmpSolution.getVehiclesDual();
    for (double fleetSizeDual : rmpSolution.getFleetSizeDuals()) {
      initialCost -= fleetSizeDual;
    }
    return initialCost;
  }

  private double getObjValue(
      Route path, Map<Integer, Double> dualValues, RMPLinearSolution rmpSolution) {
    double sum = path.getCustomersServed().stream().mapToDouble(dualValues::get).sum();
    return path.getCost() - sum + getInitialCost(rmpSolution);
  }

  private double getMinObjValue(RMPLinearSolution rmpSolution) {
    Map<Integer, Double> dualValues = new HashMap<>();
    for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
      dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
    }
    return paths.stream()
        .mapToDouble(path -> getObjValue(path, dualValues, rmpSolution))
        .min()
        .orElse(0.0);
  }

  @Override
  public PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime) {
    Instant start = Instant.now();

    performBranching();

    int labelsProcessed;

    if (isMono) {
      MonoDirectionalLabelingAlgorithm algorithm =
          new MonoDirectionalLabelingAlgorithm(instance, rmpSolution, !forceExactSolution);
      paths = algorithm.run(remainingTime);
      labelsProcessed = algorithm.getLabelsProcessed();

      if (paths.isEmpty() && !solveHeuristically) {
        algorithm = new MonoDirectionalLabelingAlgorithm(instance, rmpSolution, false);
        paths = algorithm.run(Utils.getRemainingTime(start, remainingTime));
        labelsProcessed += algorithm.getLabelsProcessed();
      }
    } else {
      LabelSettingAlgorithm algorithm =
          new LabelSettingAlgorithm(instance, rmpSolution, !forceExactSolution);
      paths = algorithm.run(remainingTime);
      labelsProcessed = algorithm.getLabelsProcessed();

      if (paths.isEmpty() && !solveHeuristically) {
        algorithm = new LabelSettingAlgorithm(instance, rmpSolution, false);
        paths = algorithm.run(Utils.getRemainingTime(start, remainingTime));
        labelsProcessed += algorithm.getLabelsProcessed();
      }
    }

    forceExactSolution = false;

    return new PricingSolution(getMinObjValue(rmpSolution), paths, labelsProcessed, true);
  }

  @Override
  public void forceExactSolution() {
    this.forceExactSolution = true;
  }

  @Override
  public void performBranchOnVisitFlow(BranchOnVisitFlow branch) {}

  @Override
  public void performBranchOnFleetSize(BranchOnFleetSize branch) {}
}
