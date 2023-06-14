import java.io.IOException;
import java.util.Set;
import ilog.concert.*;
import ilog.cplex.*;

public class CplexModel {

    public static void main(String[] args) throws IOException {

        try {
            // Create the modeler/solver object
            IloCplex cplex = new IloCplex();

            IloNumVar[][] var = new IloNumVar[1][];
            IloRange[][] rng = new IloRange[1][];

            Data data = new Data("instance2k");
            buildModel(cplex, var, rng, data);

            // write model to file
            cplex.exportModel("src/resources/star_routing.lp");

            // solve the model and display the solution if one was found
            if (cplex.solve()) {
                double[] x = cplex.getValues(var[0]);
                double[] dj = cplex.getReducedCosts(var[0]);
                double[] pi = cplex.getDuals(rng[0]);
                double[] slack = cplex.getSlacks(rng[0]);

                System.out.println("Solution status = " + cplex.getStatus());
                System.out.println("Solution value  = " + cplex.getObjValue());

                int ncols = cplex.getNcols();
                for (int j = 0; j < ncols; ++j) {
                    System.out.println("Column: " + j +
                            " Value = " + x[j] +
                            " Reduced cost = " + dj[j]);
                }

                int nrows = cplex.getNrows();
                for (int i = 0; i < nrows; ++i) {
                    System.out.println("Row   : " + i +
                            " Slack = " + slack[i] +
                            " Pi = " + pi[i]);
                }
            }
            cplex.end();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

    static void buildModel(IloMPModeler model,
            IloNumVar[][] var,
            IloRange[][] rng, Data data) throws IloException {

        // IloCplex cplex = new IloCplex();

        // IloNumVar[][] var = new IloNumVar[1][];
        // IloRange[][] rng = new IloRange[1][];

        int N = data.nodes;
        int K = data.vehicles;
        Set<Integer> S = data.customers;
        int depot = data.depot;
        int Q = data.capacity;

        IloIntVar[][][] x = new IloIntVar[N][N][K];
        IloIntVar[][] y = new IloIntVar[S.size()][K];
        IloIntVar[][] u = new IloIntVar[N][K];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    x[i][j][k] = model.boolVar();
                }
            }
        }

        for (int s : S) {
            for (int k = 0; k < K; k++) {
                y[s][k] = model.boolVar();
            }

        }
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                u[i][k] = model.intVar(1, N - 1);
            }

        }

        // Objective Function
        IloLinearIntExpr objective = model.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    objective.addTerm(x[i][j][k], data.graphWeights[i][j]);
                }
            }
        }
        model.add(model.maximize(objective));

        /*
         * =============================================================================
         * Constraints
         * =============================================================================
         */

        // Vehicle leaves the node that it enters
        IloConstraint[][] flowConstraint = new IloConstraint[N][K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr inFlow = model.linearIntExpr();
                IloLinearIntExpr outFlow = model.linearIntExpr();
                for (int j = 0; j < N; j++) {
                    inFlow.addTerm(x[i][j][k], 1);
                    outFlow.addTerm(x[j][i][k], 1);
                }
                flowConstraint[i][k] = model.addEq(inFlow, outFlow);
            }
        }
        // Every customer is served by exactly one vehicle
        IloConstraint[] servingConstraint = new IloConstraint[S.size()];
        for (int s : S) {
            IloLinearIntExpr numberOfVehiclesSevingS = model.linearIntExpr();
            for (int k = 0; k < K; k++) {
                numberOfVehiclesSevingS.addTerm(y[s][k], 1);
            }
            servingConstraint[s] = model.addEq(numberOfVehiclesSevingS, 1);
        }

        // Every vehicle leaves the depot
        IloConstraint[] depotConstraint = new IloConstraint[K];
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr outgoingEdgesFromDepot = model.linearIntExpr();
            for (int j = 0; j < N; j++) {
                outgoingEdgesFromDepot.addTerm(x[depot][j][k], 1);
            }
            depotConstraint[k] = model.addEq(outgoingEdgesFromDepot, 1);
        }
        // A vehicle can only serve visited customers
        IloConstraint[][] visitConstraint = new IloConstraint[S.size()][K];
        for (int s : S) {
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr timesInNeighborhood = model.linearIntExpr();
                for (int i = 0; i < N; i++) {
                    for (int neighbor : data.neighbors.get(S)) {
                        timesInNeighborhood.addTerm(x[i][neighbor][k], 1);
                    }

                }
                visitConstraint[s][k] = model.addLe(y[s][k], timesInNeighborhood);
            }
        }
        // Capacity constraint
        IloConstraint[] capacityConstraint = new IloConstraint[K];
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr totalDemand = model.linearIntExpr();

            for (int s : S) {
                totalDemand.addTerm(y[s][k], data.demand.get(s));
            }
            capacityConstraint[k] = model.addLe(totalDemand, Q);
        }

        // Subtour Elimination Constraints (SEC)
        IloConstraint[][][] seConstraint = new IloConstraint[N][N][K];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    if (i != depot && j != depot && j != i) {
                        IloIntExpr mtz = model.sum(
                                u[i][k],
                                model.negative(u[j][k]),
                                model.prod(x[i][j][k], N - 1));
                        seConstraint[i][j][k] = model.addLe(mtz, N - 2);
                    }
                }
            }
        }
    }
}
