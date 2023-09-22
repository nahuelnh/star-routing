package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

import java.time.Duration;
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
    public PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime) {
        PulseAlgorithm pulseAlgorithm = new PulseAlgorithm(instance, rmpSolution);
        paths = pulseAlgorithm.run(remainingTime);
        double objectiveValue = paths.stream().mapToDouble(FeasiblePath::getCost).min().orElse(0);
        return new PricingSolution(IloCplex.Status.Optimal, objectiveValue, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
