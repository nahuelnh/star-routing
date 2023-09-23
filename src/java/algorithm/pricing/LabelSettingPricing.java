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
    private boolean solveHeuristically;

    public LabelSettingPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
        this.solveHeuristically = false;
    }

    public void solveHeuristically() {
        solveHeuristically = true;
    }

    private double getObjValue() {
        return paths.stream().mapToDouble(FeasiblePath::getCost).min().orElse(0.0);
    }

    @Override
    public PricingSolution solve(RestrictedMasterProblem.RMPSolution rmpSolution, Duration remainingTime) {
        Instant start = Instant.now();
        LabelSettingAlgorithm labelSettingAlgorithm = new LabelSettingAlgorithm(instance, rmpSolution, true);
        paths = labelSettingAlgorithm.run(remainingTime);
        int labelsProcessed = labelSettingAlgorithm.getLabelsProcessed();
        if (paths.isEmpty() && !solveHeuristically) {
            labelSettingAlgorithm = new LabelSettingAlgorithm(instance, rmpSolution, false);
            paths = labelSettingAlgorithm.run(Utils.getRemainingTime(start, remainingTime));
            labelsProcessed += labelSettingAlgorithm.getLabelsProcessed();
        }
        return new PricingSolution(getObjValue(), paths, labelsProcessed, true);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
