import java.util.ArrayList;
import java.util.List;

class ColumnGeneration {

    private final Instance instance;
    private final MasterProblem rmp;
    private final PricingProblem pricing;
    private final FeasibleSolutionHeuristic heuristic;

    public ColumnGeneration(Instance instance, MasterProblem rmp, PricingProblem pricingProblem, FeasibleSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance inputInstance = new Instance("instance2");
        ColumnGeneration columnGeneration = new ColumnGeneration(inputInstance, new MasterProblem(inputInstance), new PricingProblem(inputInstance), new FeasibleSolutionHeuristic(inputInstance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        List<ElementaryPath> nuevos = heuristic.run();
        while (!nuevos.isEmpty()) {
            rmp.addPaths(nuevos);
            MasterProblem.Solution rmpSolution = rmp.solveRelaxation();
            if (rmpSolution.isFeasible()) {
                PricingProblem.Solution pricingSolution = pricing.solve(rmpSolution);
                nuevos = pricingSolution.getNegativeReducedCostPaths();
            } else {
                System.out.println("Infeasible!!!!");
                return new Solution(new ArrayList<>());
            }
        }
        MasterProblem.IntegerSolution solution = rmp.solveInteger();
        return new Solution(solution.getUsedPaths());
    }
}

