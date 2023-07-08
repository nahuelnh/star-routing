import java.util.ArrayList;
import java.util.List;

class ColumnGeneration {

    private final Data data;

    public ColumnGeneration(Data data) {
        this.data = data;
    }


    public Solution solve() {
        ArrayList<Route> clusters = new ArrayList<>();
        List<Route> nuevos = new ArrayList<>();
        FeasibleSolutionHeuristic initialConfiguration = new FeasibleSolutionHeuristic();
        nuevos.addAll(initialConfiguration.run());

        long start = System.currentTimeMillis();

        while (!nuevos.isEmpty()) {
            clusters.addAll(nuevos);

            MasterProblem master = new MasterProblem(data);
            master.solve();


            PricingProblem pricing = new PricingProblem(data);
            nuevos = pricing.solve();

        }


        MasterProblem master = new MasterProblem(data);
        master.solveInteger();


        return new Solution(master.getSolution());

    }

}

