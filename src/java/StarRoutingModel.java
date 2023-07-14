import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.io.IOException;

public class StarRoutingModel {

    private final Instance instance;
    private final IloCplex cplex;
    private final IloIntVar[][][] x;
    private final IloIntVar[][] y;
    private final IloIntVar[][] u;
    private final IloConstraint[][] flowConstraint;
    private final IloConstraint[] depotConstraint;
    private final IloConstraint[][] visitConstraint;
    private final IloConstraint[] servingConstraint;
    private final IloConstraint[] capacityConstraint;
    private final IloConstraint[][][] mtzConstraints;

    public StarRoutingModel(Instance instance) throws IloException {
        int N = instance.getNodes();
        int K = instance.getVehicles();
        int S = instance.getCustomers().size();

        this.instance = instance;
        this.x = new IloIntVar[N][N][K];
        this.y = new IloIntVar[S][K];
        this.u = new IloIntVar[N][K];
        this.flowConstraint = new IloConstraint[N][K];
        this.depotConstraint = new IloConstraint[K];
        this.visitConstraint = new IloConstraint[S][K];
        this.servingConstraint = new IloConstraint[S];
        this.capacityConstraint = new IloConstraint[K];
        this.mtzConstraints = new IloConstraint[N][N][K];
        this.cplex = new IloCplex();
    }

    public static void main(String[] args) throws IOException {
        try {
            Instance inputInstance = new Instance("instance4");
            StarRoutingModel starRoutingModel = new StarRoutingModel(inputInstance);
            starRoutingModel.buildModel();
            starRoutingModel.run();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

    private void run() throws IloException {
        // write model to file
        cplex.exportModel("src/resources/star_routing.lp");

        // solve the model and display the solution if one was found
        if (cplex.solve()) {
            int N = instance.getNodes();
            int K = instance.getVehicles();
            int S = instance.getCustomers().size();

            System.out.println("Solution status = " + cplex.getStatus());
            System.out.println("Solution value  = " + cplex.getObjValue());
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    for (int k = 0; k < K; k++) {
                        if (cplex.getValue(x[i][j][k]) > 0) {
                            System.out.println("x " + i + " " + j + " " + k + " = " + cplex.getValue(x[i][j][k]));

                        }
                    }
                }
            }
            for (int s = 0; s < S; s++) {
                for (int k = 0; k < K; k++) {
                    if (cplex.getValue(y[s][k]) > 0) {
                        System.out.println("y " + s + " " + k + " = " + cplex.getValue(y[s][k]));
                    }

                }
            }
            for (int i = 0; i < N; i++) {
                for (int k = 0; k < K; k++) {
                    if (i != instance.getDepot() && cplex.getValue(u[i][k]) > 0) {
                        System.out.println("u " + i + " " + k + " = " + cplex.getValue(u[i][k]));
                    }
                }
            }
        }
        cplex.end();
    }

    private void buildModel() throws IloException {

        int N = instance.getNodes();
        int K = instance.getVehicles();
        int S = instance.getCustomers().size();
        int depot = instance.getDepot();
        int Q = instance.getCapacity();

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    x[i][j][k] = cplex.boolVar();
                }
            }
        }

        for (int s = 0; s < S; s++) {
            for (int k = 0; k < K; k++) {
                y[s][k] = cplex.boolVar();
            }

        }
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                u[i][k] = cplex.intVar(1, N - 1);
            }

        }

        // Objective Function
        IloLinearIntExpr objective = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    objective.addTerm(x[i][j][k], instance.getGraphWeights(i, j));
                }
            }
        }
        cplex.addMinimize(objective);

        /*
         * =============================================================================
         * Constraints
         * =============================================================================
         */

        // Vehicle leaves the node that it enters
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr inFlow = cplex.linearIntExpr();
                IloLinearIntExpr outFlow = cplex.linearIntExpr();
                for (int j = 0; j < N; j++) {
                    inFlow.addTerm(x[i][j][k], 1);
                    outFlow.addTerm(x[j][i][k], 1);
                }
                flowConstraint[i][k] = cplex.addEq(inFlow, outFlow);
            }
        }
        // Every customer is served by exactly one vehicle
        for (int s = 0; s < S; s++) {
            IloLinearIntExpr numberOfVehiclesSevingS = cplex.linearIntExpr();
            for (int k = 0; k < K; k++) {
                numberOfVehiclesSevingS.addTerm(y[s][k], 1);
            }
            servingConstraint[s] = cplex.addEq(numberOfVehiclesSevingS, 1);
        }

        // Every vehicle leaves the depot
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr outgoingEdgesFromDepot = cplex.linearIntExpr();
            for (int j = 0; j < N; j++) {
                outgoingEdgesFromDepot.addTerm(x[depot][j][k], 1);
            }
            depotConstraint[k] = cplex.addEq(outgoingEdgesFromDepot, 1);
        }
        // A vehicle can only serve visited customers
        for (int s = 0; s < S; s++) {
            int currentCustomer = instance.getCustomers().get(s);
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr timesInNeighborhood = cplex.linearIntExpr();
                for (int i = 0; i < N; i++) {
                    for (int neighbor : instance.getNeighbors().get(currentCustomer)) {
                        timesInNeighborhood.addTerm(x[i][neighbor][k], 1);
                    }

                }
                visitConstraint[s][k] = cplex.addLe(y[s][k], timesInNeighborhood);
            }
        }
        // Capacity constraint
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr totalDemand = cplex.linearIntExpr();
            for (int s = 0; s < S; s++) {
                totalDemand.addTerm(y[s][k], instance.getDemand(instance.getCustomers().get(s)));
            }
            capacityConstraint[k] = cplex.addLe(totalDemand, Q);
        }

        // Subtour Elimination Constraints (SEC)
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    if (i != depot && j != depot && j != i) {
                        IloIntExpr mtz = cplex.sum(
                                u[i][k],
                                cplex.negative(u[j][k]),
                                cplex.prod(x[i][j][k], N - 1));
                        mtzConstraints[i][j][k] = cplex.addLe(mtz, N - 2);
                    }
                }
            }
        }
    }
}
