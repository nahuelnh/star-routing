package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class PulsePricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;

    public PulsePricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        PulseAlgorithm pulseAlgorithm = new PulseAlgorithm(instance, rmpSolution);
        paths = pulseAlgorithm.run();
        double objectiveValue =
                paths.stream().mapToDouble(x -> x.getCost() - rmpSolution.getVehiclesDual()).min().orElse(0);
        return new Solution(IloCplex.Status.Optimal, objectiveValue, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
