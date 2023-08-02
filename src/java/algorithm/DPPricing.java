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
        paths = new ESPPRCAlgorithmDP(instance, rmpSolution).run();
        System.out.println(paths);
        assert false;
        return new Solution(IloCplex.Status.Optimal, 0.0, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
