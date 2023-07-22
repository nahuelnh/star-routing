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
        Instance instance = new Instance("instance_neighbors_40", true);
        ColumnGeneration columnGeneration = new ColumnGeneration(instance, new MTZRestrictedMasterProblem(instance),
                new SecondPricingProblem(instance), new FeasibleSolutionHeuristic(instance));
        Solution solution = columnGeneration.solve();
        System.out.println(solution);
    }

    public Solution solve() {
        List<ElementaryPath> newPaths = heuristic.run();
        double relaxationOptimal = Double.MAX_VALUE;
        while (!newPaths.isEmpty()) {
            rmp.addPaths(newPaths);
            RestrictedMasterProblem.Solution rmpSolution = rmp.solveRelaxation();
            relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());
            PricingProblem.Solution pricingSolution = pricing.solve(rmpSolution);
            newPaths = pricingSolution.getNegativeReducedCostPaths();
        }
        System.out.println("Relaxation optimal:" + relaxationOptimal);
        RestrictedMasterProblem.IntegerSolution solution = rmp.solveInteger();
        System.out.println("Integer optimal:" + solution.getObjectiveValue());
        return new Solution(solution.getUsedPaths());
    }
}

