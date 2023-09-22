package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompactModel {

    private final Instance instance;
    private IloCplex cplex;
    private IloNumVar[][][] x;
    private IloNumVar[][] y;
    private IloNumVar[][] u;

    public CompactModel(Instance instance) {
        this.instance = instance;
    }

    public static void main(String[] args) {
        Instance instance = new Instance("instance_rptd_path", true);
        CompactModel starRoutingModel = new CompactModel(instance);
        starRoutingModel.solve();
    }

    private Solution solve(boolean integral, Duration timeout) {
        Instant start = Instant.now();
        try {
            buildModel(integral, timeout);
            cplex.solve();
            Duration elapsedTime = Utils.getElapsedTime(start);
            cplex.writeSolution("src/resources/star_routing_model.sol");
            Solution.Status status = IloCplex.Status.Optimal.equals(cplex.getStatus()) ? Solution.Status.FINISHED :
                    Solution.Status.TIMEOUT;
            List<FeasiblePath> paths =
                    integral && Solution.Status.FINISHED.equals(status) ? getPathsFromSolution() : new ArrayList<>();
            Solution solution = new Solution(status, cplex.getObjValue(), paths, elapsedTime);
            cplex.end();
            return solution;
        } catch (IloException e) {
            cplex.end();
            return new Solution(Solution.Status.TIMEOUT, 0.0, new ArrayList<>(), Utils.getElapsedTime(start));
        }

    }

    public Solution solve() {
        return solve(true, Utils.DEFAULT_TIMEOUT);
    }

    public Solution solve(Duration timeout) {
        return solve(true, timeout);
    }

    public Solution solveRelaxation() {
        return solve(false, Utils.DEFAULT_TIMEOUT);
    }

    public Solution solveRelaxation(Duration timeout) {
        return solve(false, timeout);
    }

    private void createVariables(boolean integral) throws IloException {
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        int S = instance.getNumberOfCustomers();
        x = new IloIntVar[N][N][K];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    x[i][j][k] = integral ? cplex.boolVar("x_" + i + "_" + j + "_" + k) :
                            cplex.numVar(0, 1, "x_" + i + "_" + j + "_" + k);
                }
            }
        }
        y = new IloIntVar[S][K];
        for (int s = 0; s < S; s++) {
            for (int k = 0; k < K; k++) {
                y[s][k] = integral ? cplex.boolVar("y_" + s + "_" + k) : cplex.numVar(0, 1, "y_" + s + "_" + k);
            }

        }
        u = new IloIntVar[N][K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                u[i][k] = integral ? cplex.intVar(1, N - 1, "u_" + i + "_" + k) :
                        cplex.numVar(1, N - 1, "u_" + i + "_" + k);
            }
        }

        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                cplex.addEq(x[i][i][k], 0);
            }
        }
    }

    private void createFlowConstraints() throws IloException {
        // Vehicle leaves the node that it enters
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                IloLinearNumExpr inFlow = cplex.linearNumExpr();
                IloLinearNumExpr outFlow = cplex.linearNumExpr();
                for (int j = 0; j < N; j++) {
                    inFlow.addTerm(x[i][j][k], 1);
                    outFlow.addTerm(x[j][i][k], 1);
                }
                cplex.addEq(inFlow, outFlow, "flow_" + i + "_" + k);
            }
        }
    }

    private void createServingConstraints() throws IloException {
        // Every customer is served by exactly one vehicle
        int S = instance.getNumberOfCustomers();
        for (int s = 0; s < S; s++) {
            IloNumExpr numberOfVehiclesServingS = Utils.getRowSum(cplex, y, s);
            cplex.addEq(numberOfVehiclesServingS, 1, "serving_" + s);
        }
    }

    private void createDepotConstraints() throws IloException {
        // Every vehicle leaves the depot
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        for (int k = 0; k < K; k++) {
            IloLinearNumExpr outgoingEdgesFromDepot = cplex.linearNumExpr();
            for (int j = 0; j < N; j++) {
                outgoingEdgesFromDepot.addTerm(x[instance.getDepot()][j][k], 1);
            }
            if (instance.unusedVehiclesAllowed()) {
                cplex.addLe(outgoingEdgesFromDepot, 1, "depot_" + k);
            } else {
                cplex.addEq(outgoingEdgesFromDepot, 1, "depot_" + k);
            }
        }
    }

    private void createCapacityConstraints() throws IloException {
        // Capacity constraint
        int K = instance.getNumberOfVehicles();
        int S = instance.getNumberOfCustomers();
        for (int k = 0; k < K; k++) {
            IloLinearNumExpr totalDemand = cplex.linearNumExpr();
            for (int s = 0; s < S; s++) {
                totalDemand.addTerm(y[s][k], instance.getDemand(instance.getCustomer(s)));
            }
            cplex.addLe(totalDemand, instance.getCapacity(), "capacity_" + k);
        }
    }

    private void createMTZConstraints() throws IloException {
        // Subtour Elimination Constraints (SEC)
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < K; k++) {
                    if (i != instance.getDepot() && j != instance.getDepot() && j != i) {
                        IloNumExpr mtz = cplex.sum(u[i][k], cplex.negative(u[j][k]), cplex.prod(x[i][j][k], N - 1));
                        cplex.addLe(mtz, N - 2, "mtz_" + i + "_" + j + "_" + k);
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
        for (int s = 0; s < S; s++) {
            int currentCustomer = instance.getCustomer(s);
            for (int k = 0; k < K; k++) {
                IloLinearNumExpr timesInNeighborhood = cplex.linearNumExpr();
                for (int i = 0; i < N; i++) {
                    for (int neighbor : instance.getNeighbors(currentCustomer)) {
                        timesInNeighborhood.addTerm(x[i][neighbor][k], 1);
                    }
                }
                cplex.addLe(y[s][k], timesInNeighborhood, "visit_" + s + "_" + k);
            }
        }
    }

    private void createObjective() throws IloException {
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != j) {
                    for (int k = 0; k < K; k++) {
                        objective.addTerm(x[i][j][k], instance.getEdgeWeight(i, j));
                    }
                }
            }
        }
        cplex.addMinimize(objective);
    }

    private void buildModel(boolean integral, Duration timeout) throws IloException {
        cplex = new IloCplex();
        cplex.setParam(IloCplex.Param.Output.WriteLevel, IloCplex.WriteLevel.NonzeroVars);
        cplex.setParam(IloCplex.Param.TimeLimit, timeout.getSeconds());
        cplex.setOut(null);
        createVariables(integral);
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
            if (Utils.getBoolValue(cplex, x[from][i][vehicle])) {
                return i;
            }
        }
        throw new AssertionError(String.format("Path starting in %d has no end", from));
    }

    private Set<Integer> getVisitedCustomers(int vehicle) throws IloException {
        Set<Integer> visitedCustomers = new HashSet<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            if (Utils.getBoolValue(cplex, y[s][vehicle])) {
                visitedCustomers.add(instance.getCustomer(s));
            }
        }
        return visitedCustomers;
    }

    private boolean isVehicleUsed(int k) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                if (Utils.getBoolValue(cplex, x[i][j][k])) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<FeasiblePath> getPathsFromSolution() throws IloException {
        List<FeasiblePath> ret = new ArrayList<>();
        for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
            if (isVehicleUsed(k)) {
                FeasiblePath path = new FeasiblePath();
                int lastNode = instance.getDepot();
                int currentNode = getNextNodeInPath(lastNode, k);
                while (currentNode != instance.getDepot()) {
                    path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
                    lastNode = currentNode;
                    currentNode = getNextNodeInPath(currentNode, k);
                }
                path.addNode(instance.getDepot(), instance.getEdgeWeight(lastNode, currentNode));
                path.addCustomers(getVisitedCustomers(k));
                ret.add(path);
            }
        }
        return ret;
    }
}
