package algorithm.branching;

import algorithm.RMPLinearSolution;
import commons.Instance;

import java.util.ArrayList;
import java.util.List;

public class BranchingRuleManager {

    private static final double EPSILON = 0.01;
    private final Instance instance;

    public BranchingRuleManager(Instance instance) {
        this.instance = instance;
    }

    public List<Branch> getBranches(RMPLinearSolution rmpSolution) {
        List<Branch> ret = new ArrayList<>();
        double maxFractionalPart = 0.0;
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                if (i != j) {
                    double flow = rmpSolution.getFlow(i, j);
                    double fractionalPart = Math.abs(flow - (int) (flow + 0.5));
                    if (fractionalPart > EPSILON && fractionalPart > maxFractionalPart) {
                        maxFractionalPart = fractionalPart;
                        ret = List.of(new BranchOnEdge(i, j, (int) Math.floor(flow), Branch.Direction.DOWN),
                                new BranchOnEdge(i, j, (int) Math.ceil(flow), Branch.Direction.UP));
                    }
                }
            }
        }
        return ret;
    }

    public List<Branch> getBranches(RMPLinearSolution rmpSolution, boolean ignore) {
        List<Branch> ret = new ArrayList<>();
        double maxFractionalPart = 0.0;
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                if (i != j) {
                    double flow = rmpSolution.getFlow(i, j);
                    double fractionalPart = Math.abs(flow - (int) (flow + 0.5));
                    if (fractionalPart > EPSILON && fractionalPart > maxFractionalPart) {
                        maxFractionalPart = fractionalPart;
                        ret = List.of(new BranchOnEdge(i, j, (int) Math.floor(flow), Branch.Direction.DOWN),
                                new BranchOnEdge(i, j, (int) Math.ceil(flow), Branch.Direction.UP));
                    }
                }
            }
        }
        return ret;
    }

}
