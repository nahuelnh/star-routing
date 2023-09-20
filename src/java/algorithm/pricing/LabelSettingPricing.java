package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

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
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        paths = new LabelSettingAlgorithm(instance, rmpSolution, applyRelaxedDominance, applyFakeCostHeuristic).run();
        return new Solution(IloCplex.Status.Optimal, 0.0, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
