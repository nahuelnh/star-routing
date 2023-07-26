package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class ESPPRCPricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;

    public ESPPRCPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        PulseAlgorithm pulseAlgorithm = new PulseAlgorithm(instance, rmpSolution);
        paths = pulseAlgorithm.getOptimalPaths();
        double cost = paths.stream().mapToInt(FeasiblePath::getCost).sum();
        return new Solution(IloCplex.Status.Optimal, cost, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
