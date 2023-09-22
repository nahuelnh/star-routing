package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LabelSettingPricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;
    private boolean applyRelaxedDominance;
    private boolean applyFakeCostHeuristic;

    public LabelSettingPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
        this.applyRelaxedDominance = false;
        this.applyFakeCostHeuristic = false;
    }

    public void applyRelaxedDominance() {
        applyRelaxedDominance = true;
    }

    public void applyFakeCostHeuristic() {
        applyFakeCostHeuristic = true;
    }

    @Override
    public PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime) {
        paths = new LabelSettingAlgorithm(instance, rmpSolution, applyRelaxedDominance, applyFakeCostHeuristic).run(
                remainingTime);
        double objValue = paths.stream().mapToDouble(FeasiblePath::getCost).min().orElse(0.0);
        return new PricingSolution(objValue, paths, true);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
