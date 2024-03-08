package algorithm;

import commons.Instance;

import java.util.ArrayList;
import java.util.List;

public class BranchingRuleManager {

    private static final double EPSILON = 0.01;
    private final Instance instance;

    public BranchingRuleManager(Instance instance) {
        this.instance = instance;
    }

    public List<BranchingDirection> getBranches(RestrictedMasterProblem.RMPSolution rmpSolution) {

        List<BranchingDirection> ret = new ArrayList<>();
        double maxFractionalPart = 0.0;
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                if (i != j) {
                    double flux = rmpSolution.getFlux(i, j);
                    double fractionalPart = Math.abs(flux - (int) (flux + 0.5));
                    if (fractionalPart > EPSILON && fractionalPart > maxFractionalPart) {
                        maxFractionalPart = fractionalPart;
                        ret = List.of(new BranchOnEdge(i, j, (int) Math.floor(flux), false),
                                new BranchOnEdge(i, j, (int) Math.ceil(flux), true));
                    }
                }
            }
        }

        return ret;
    }

}
