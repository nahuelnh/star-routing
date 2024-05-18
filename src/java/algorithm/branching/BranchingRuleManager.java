package algorithm.branching;

import algorithm.RMPLinearSolution;
import commons.Instance;
import commons.VisitFlow;

import java.util.ArrayList;
import java.util.List;

public class BranchingRuleManager {

    private static final double EPSILON = 1e-6d;
    private final Instance instance;

    public BranchingRuleManager(Instance instance) {
        this.instance = instance;
    }

    public List<Branch> applyBranchingRules(RMPLinearSolution rmpSolution) {
        List<Branch> ret = new ArrayList<>();
        double maxFractionalPart = 0.0;
        for (VisitFlow visitFlow : rmpSolution.getVisitFlow()) {
            double flow = visitFlow.value();
            double fractionalPart = Math.abs(flow - (int) (flow + 0.5));
            if (fractionalPart > EPSILON && fractionalPart > maxFractionalPart) {
                maxFractionalPart = fractionalPart;
                ret = List.of(new BranchOnVisitFlow(visitFlow.edge(), visitFlow.customer(), 0, Branch.Direction.DOWN),
                        new BranchOnVisitFlow(visitFlow.edge(), visitFlow.customer(), 1, Branch.Direction.UP));
            }
        }
        return ret;
    }
}
