package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LabelSettingPricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;
    private boolean applyHeuristics;

    public LabelSettingPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
        this.applyHeuristics = false;
    }

    public void applyHeuristics() {
        applyHeuristics = true;
    }

    @Override
    public PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime) {
        Instant start = Instant.now();
        paths = new LabelSettingAlgorithm(instance, rmpSolution, true).run(remainingTime);
        if (paths.isEmpty() && !applyHeuristics) {
            paths = new LabelSettingAlgorithm(instance, rmpSolution, false).run(
                    Utils.getRemainingTime(start, remainingTime));
        }
        double objValue = paths.stream().mapToDouble(FeasiblePath::getCost).min().orElse(0.0);
        return new PricingSolution(objValue, paths, true);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
