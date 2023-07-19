import java.util.ArrayList;
import java.util.List;

class ColumnGeneration {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final FeasibleSolutionHeuristic heuristic;

    public ColumnGeneration(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem, FeasibleSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance inputInstance = new Instance("instance5");
        ColumnGeneration columnGeneration = new ColumnGeneration(inputInstance, new RestrictedMasterProblem(inputInstance), new PricingProblem(inputInstance), new FeasibleSolutionHeuristic(inputInstance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        List<ElementaryPath> nuevos = heuristic.run();
        while (!nuevos.isEmpty()) {
            rmp.addPaths(nuevos);
            RestrictedMasterProblem.Solution rmpSolution = rmp.solveRelaxation();
            if (rmpSolution.isFeasible()) {
                PricingProblem.Solution pricingSolution = pricing.solve(rmpSolution);
                nuevos = pricingSolution.getNegativeReducedCostPaths();
            } else {
                System.out.println("Infeasible!!!!");
                return new Solution(new ArrayList<>());
            }
        }
        RestrictedMasterProblem.IntegerSolution solution = rmp.solveInteger();
        return new Solution(solution.getUsedPaths());
    }
}

