import ilog.concert.IloException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ColumnGeneration {

    private final Instance instance;

    public ColumnGeneration(Instance instance) {
        this.instance = instance;
    }

    public static void main(String[] args) throws IOException {
        try {
            Instance inputInstance = new Instance("instance1");
            ColumnGeneration columnGeneration = new ColumnGeneration(inputInstance);
            columnGeneration.solve();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

    public Solution solve() throws IloException {
        ArrayList<Route> routes = new ArrayList<>();
        List<Route> nuevos = new ArrayList<>();
        FeasibleSolutionHeuristic initialConfiguration = new FeasibleSolutionHeuristic(instance);
        nuevos.addAll(initialConfiguration.run());
        while (!nuevos.isEmpty()) {
            routes.addAll(nuevos);
            MasterProblem master = new MasterProblem(instance, routes);
            MasterProblem.Solution solution = master.solveRelaxation();

            PricingProblem pricing = new PricingProblem(instance, solution);
            nuevos = new ArrayList<>();
            Route newRoute = pricing.solve();
            if (newRoute == null) {
                break;
            }
            nuevos.add(newRoute);
        }
        MasterProblem master = new MasterProblem(instance, routes);
        MasterProblem.Solution solution = master.solveInteger();
        return new Solution(new ArrayList<>());
    }

}

