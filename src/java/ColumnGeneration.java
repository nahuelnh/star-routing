import java.util.List;

class ColumnGeneration {

    private final Instance instance;
    private final RestrictedMasterProblem rmp;
    private final PricingProblem pricing;
    private final FeasibleSolutionHeuristic heuristic;

    public ColumnGeneration(Instance instance, RestrictedMasterProblem rmp, PricingProblem pricingProblem,
                            FeasibleSolutionHeuristic heuristic) {
        this.instance = instance;
        this.rmp = rmp;
        this.pricing = pricingProblem;
        this.heuristic = heuristic;
    }

    public static void main(String[] args) {
        Instance inputInstance = new Instance("instance5", true);
        ColumnGeneration columnGeneration =
                new ColumnGeneration(inputInstance, new RestrictedMasterProblem(inputInstance),
                        new PricingProblem(inputInstance), new FeasibleSolutionHeuristic(inputInstance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        List<ElementaryPath> newPaths = heuristic.run();
        while (!newPaths.isEmpty()) {
            rmp.addPaths(newPaths);
            RestrictedMasterProblem.Solution rmpSolution = rmp.solveRelaxation();
            PricingProblem.Solution pricingSolution = pricing.solve(rmpSolution);
            newPaths = pricingSolution.getNegativeReducedCostPaths();
        }
        RestrictedMasterProblem.IntegerSolution solution = rmp.solveInteger();
        return new Solution(solution.getUsedPaths());
    }
}

