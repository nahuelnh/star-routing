import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StarRoutingModel {

    private final Instance instance;
    private IloCplex cplex;
    private IloIntVar[][][] x;
    private IloIntVar[][] y;
    private IloIntVar[][] u;
    private IloConstraint[][] flowConstraint;
    private IloConstraint[] depotConstraint;
    private IloConstraint[][] visitConstraint;
    private IloConstraint[] servingConstraint;
    private IloConstraint[] capacityConstraint;
    private IloConstraint[][][] mtzConstraints;

    public StarRoutingModel(Instance instance) throws IloException {
        this.instance = instance;
    }

    public static void main(String[] args) {
        try {
            Instance instance = new Instance("instance_rptd_path", true);
            StarRoutingModel starRoutingModel = new StarRoutingModel(instance);
            starRoutingModel.solve();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

    public Solution solve() throws IloException {
        buildModel();
        cplex.solve();
        cplex.writeSolution("src/resources/star_routing_model.sol");
        Solution solution = new Solution(getPathsFromSolution());
        cplex.end();
        return solution;
    }

    private void createVariables() throws IloException {
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        int S = instance.getNumberOfCustomers();
        x = new IloIntVar[N][N][K];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    x[i][j][k] = cplex.boolVar("x_" + i + "_" + j + "_" + k);
                }
            }
        }
        y = new IloIntVar[S][K];
        for (int s = 0; s < S; s++) {
            for (int k = 0; k < K; k++) {
                y[s][k] = cplex.boolVar("y_" + s + "_" + k);
            }

        }
        u = new IloIntVar[N][K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                u[i][k] = cplex.intVar(1, N - 1, "u_" + i + "_" + k);
            }
        }
    }

    private void createFlowConstraints() throws IloException {
        // Vehicle leaves the node that it enters
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        flowConstraint = new IloConstraint[N][K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr inFlow = cplex.linearIntExpr();
                IloLinearIntExpr outFlow = cplex.linearIntExpr();
                for (int j = 0; j < N; j++) {
                    inFlow.addTerm(x[i][j][k], 1);
                    outFlow.addTerm(x[j][i][k], 1);
                }
                flowConstraint[i][k] = cplex.addEq(inFlow, outFlow, "flow_" + i + "_" + k);
            }
        }
    }

    private void createServingConstraints() throws IloException {
        // Every customer is served by exactly one vehicle
        int S = instance.getNumberOfCustomers();
        servingConstraint = new IloConstraint[S];
        for (int s = 0; s < S; s++) {
            IloIntExpr numberOfVehiclesServingS = Utils.getIntArraySum(cplex, y[s]);
            servingConstraint[s] = cplex.addEq(numberOfVehiclesServingS, 1, "serving_" + s);
        }
    }

    private void createDepotConstraints() throws IloException {
        // Every vehicle leaves the depot
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        depotConstraint = new IloConstraint[K];
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr outgoingEdgesFromDepot = cplex.linearIntExpr();
            for (int j = 0; j < N; j++) {
                outgoingEdgesFromDepot.addTerm(x[instance.getDepot()][j][k], 1);
            }
            if (instance.unusedVehiclesAllowed()) {
                depotConstraint[k] = cplex.addLe(outgoingEdgesFromDepot, 1, "depot_" + k);
            } else {
                depotConstraint[k] = cplex.addEq(outgoingEdgesFromDepot, 1, "depot_" + k);
            }
        }
    }

    private void createCapacityConstraints() throws IloException {
        // Capacity constraint
        int K = instance.getNumberOfVehicles();
        int S = instance.getNumberOfCustomers();
        capacityConstraint = new IloConstraint[K];
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr totalDemand = cplex.linearIntExpr();
            for (int s = 0; s < S; s++) {
                totalDemand.addTerm(y[s][k], instance.getDemand(instance.getCustomers().get(s)));
            }
            capacityConstraint[k] = cplex.addLe(totalDemand, instance.getCapacity(), "capacity_" + k);
        }
    }

    private void createMTZConstraints() throws IloException {
        // Subtour Elimination Constraints (SEC)
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        mtzConstraints = new IloConstraint[N][N][K];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    if (i != instance.getDepot() && j != instance.getDepot() && j != i) {
                        IloIntExpr mtz = cplex.sum(u[i][k], cplex.negative(u[j][k]), cplex.prod(x[i][j][k], N - 1));
                        mtzConstraints[i][j][k] = cplex.addLe(mtz, N - 2, "mtz_" + i + "_" + j + "_" + k);
                    }
                }
            }
        }

    }

    private void createVisitConstraints() throws IloException {
        // A vehicle can only serve visited customers
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        int S = instance.getNumberOfCustomers();
        visitConstraint = new IloConstraint[S][K];
        for (int s = 0; s < S; s++) {
            int currentCustomer = instance.getCustomer(s);
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr timesInNeighborhood = cplex.linearIntExpr();
                for (int i = 0; i < N; i++) {
                    for (int neighbor : instance.getNeighbors(currentCustomer)) {
                        timesInNeighborhood.addTerm(x[i][neighbor][k], 1);
                    }
                }
                visitConstraint[s][k] = cplex.addLe(y[s][k], timesInNeighborhood, "visit_" + s + "_" + k);
            }
        }
    }

    private void createObjective() throws IloException {
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        IloLinearIntExpr objective = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    objective.addTerm(x[i][j][k], instance.getEdgeWeight(i, j));
                }
            }
        }
        cplex.addMinimize(objective);
    }

    private void buildModel() throws IloException {
        cplex = new IloCplex();
        cplex.setParam(IloCplex.Param.Output.WriteLevel, IloCplex.WriteLevel.NonzeroVars);
        cplex.setOut(null);
        createVariables();
        createFlowConstraints();
        createServingConstraints();
        createDepotConstraints();
        createVisitConstraints();
        createCapacityConstraints();
        createMTZConstraints();
        createObjective();
        cplex.exportModel("src/resources/star_routing.lp");
    }

    private int getNextNodeInPath(int from, int vehicle) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (Math.round(cplex.getValue(x[from][i][vehicle])) == 1) {
                return i;
            }
        }
        throw new AssertionError(String.format("Path starting in %d has no end", from));
    }

    private Set<Integer> getCustomersServed(int currentNode, int vehicle, Set<Integer> alreadyVisited)
            throws IloException {
        Set<Integer> customersServed = new HashSet<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            int customer = instance.getCustomer(s);
            if (!alreadyVisited.contains(customer) && instance.getNeighbors(customer).contains(currentNode) &&
                    Math.round(cplex.getValue(y[s][vehicle])) == 1) {
                customersServed.add(customer);
                alreadyVisited.add(customer);
            }
        }
        return customersServed;
    }

    private boolean isVehicleUsed(int k) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                if (Math.round(cplex.getValue(x[i][j][k])) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ElementaryPath> getPathsFromSolution() throws IloException {
        List<ElementaryPath> ret = new ArrayList<>();
        for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
            if (isVehicleUsed(k)) {
                ElementaryPath path = ElementaryPath.emptyPath();
                Set<Integer> alreadyVisited = new HashSet<>();
                int lastNode = instance.getDepot();
                int currentNode = getNextNodeInPath(lastNode, k);
                while (currentNode != instance.getDepot()) {
                    path.addNode(currentNode, getCustomersServed(currentNode, k, alreadyVisited),
                            instance.getEdgeWeight(lastNode, currentNode));
                    lastNode = currentNode;
                    currentNode = getNextNodeInPath(currentNode, k);
                }
                path.addNode(instance.getDepot(), new HashSet<>(), instance.getEdgeWeight(lastNode, currentNode));
                ret.add(path);
            }
        }
        return ret;
    }
}
