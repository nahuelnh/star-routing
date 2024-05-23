package algorithm.branching;

import algorithm.RMPLinearSolution;
import commons.Instance;
import commons.VisitFlow;

import java.util.ArrayList;
import java.util.List;

public class BranchingRuleManager {

    private static final double EPSILON = 1e-6d;

    public BranchingRuleManager() {
    }

    private static double getFractionalPart(double d) {
        return Math.abs(d - (int) (d + 0.5));
    }

    public List<Branch> applyBranchingRules(RMPLinearSolution rmpSolution) {
        List<Branch> ret = new ArrayList<>();

        double fractionalPart = getFractionalPart(rmpSolution.getNumberOfVehicles());
        if (fractionalPart > EPSILON) {
            ret = List.of(new BranchOnFleetSize((int) Math.floor(rmpSolution.getNumberOfVehicles()), Branch.Direction.DOWN),
                    new BranchOnFleetSize((int) Math.ceil(rmpSolution.getNumberOfVehicles()), Branch.Direction.UP)
            );
        }

        double maxFractionalPart = 0.0;
        for (VisitFlow visitFlow : rmpSolution.getVisitFlow()) {
            double flow = visitFlow.value();
            fractionalPart = getFractionalPart(flow);
            if (fractionalPart > EPSILON && fractionalPart > maxFractionalPart) {
                maxFractionalPart = fractionalPart;
                ret = List.of(new BranchOnVisitFlow(visitFlow.edge(), visitFlow.customer(), 0, Branch.Direction.DOWN),
                        new BranchOnVisitFlow(visitFlow.edge(), visitFlow.customer(), 1, Branch.Direction.UP));
            }
        }

        return ret;
    }
}
