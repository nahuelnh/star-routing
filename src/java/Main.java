import ilog.concert.IloException;

public class Main {
    public static void main(String[] args) {
        runInstance("instance_rptd_path");
        runInstance("instance_2v1");
        runInstance("instance_2v2");
        runInstance("instance_large");
        runInstance("instance_simple");
        runInstance("instance_neighbors_20");
        runInstance("instance_neighbors_40");
        runInstance("instance_random_20");
        runInstance("instance_random_40");

    }

    private static void runInstance(String instanceName) {
        try {
            System.out.println("Running instance: " + instanceName);
            Instance instance = new Instance(instanceName, true);
            StarRoutingModel starRoutingModel = new StarRoutingModel(instance);
            System.out.println(starRoutingModel.solve());
            ColumnGeneration columnGeneration = new ColumnGeneration(instance, new RestrictedMasterProblem(instance),
                    new SecondPricingProblem(instance), new FeasibleSolutionHeuristic(instance));
            System.out.println(columnGeneration.solve());
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

}
