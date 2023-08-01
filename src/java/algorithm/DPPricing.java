package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class DPPricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;


    public DPPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        return new Solution(IloCplex.Status.Optimal, 0.0, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return null;
    }
}
