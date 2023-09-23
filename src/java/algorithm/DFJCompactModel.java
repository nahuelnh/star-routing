package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Solution;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DFJCompactModel {

    private final Instance instance;
    private IloCplex cplex;
    private IloIntVar[][][] x;
    private IloIntVar[][] y;

    public DFJCompactModel(Instance instance) {
        this.instance = instance;
    }

    public static void main(String[] args) {
        try {
            Instance instance = new Instance("instance_large", true);
            DFJCompactModel starRoutingModel = new DFJCompactModel(instance);
            starRoutingModel.solve();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }

    public Solution solve() throws IloException {
        Instant start = Instant.now();
        buildModel();
        cplex.solve();
        Instant finish = Instant.now();
        Solution.Status status =
                cplex.getStatus() == IloCplex.Status.Error ? Solution.Status.TIMEOUT : Solution.Status.OPTIMAL;
        Solution solution =
                new Solution(status, cplex.getObjValue(), getPathsFromSolution(), Duration.between(start, finish));
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
    }

    private void createFlowConstraints() throws IloException {
        // Vehicle leaves the node that it enters
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                IloLinearIntExpr inFlow = cplex.linearIntExpr();
                IloLinearIntExpr outFlow = cplex.linearIntExpr();
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
            IloIntExpr numberOfVehiclesServingS = Utils.getRowSum(cplex, y, s);
            cplex.addEq(numberOfVehiclesServingS, 1, "serving_" + s);
        }
    }

    private void createDepotConstraints() throws IloException {
        // Every vehicle leaves the depot
        int N = instance.getNumberOfNodes();
        int K = instance.getNumberOfVehicles();
        for (int k = 0; k < K; k++) {
            IloLinearIntExpr outgoingEdgesFromDepot = cplex.linearIntExpr();
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
            IloLinearIntExpr totalDemand = cplex.linearIntExpr();
            for (int s = 0; s < S; s++) {
                totalDemand.addTerm(y[s][k], instance.getDemand(instance.getCustomer(s)));
            }
            cplex.addLe(totalDemand, instance.getCapacity(), "capacity_" + k);
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
                IloLinearIntExpr timesInNeighborhood = cplex.linearIntExpr();
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
        createObjective();
        cplex.use(new DFJConstraintsCallback());
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

    private class DFJConstraintsCallback extends IloCplex.LazyConstraintCallback {

        private int[] connectedComponent;

        public DFJConstraintsCallback() {
            this.connectedComponent = new int[instance.getNumberOfNodes()];
        }

        @Override
        protected void main() throws IloException {
            for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                computeConnectedComponents(k);
                Map<Integer, Set<Integer>> forbiddenCycles = getForbiddenCycles();
                for (int ccIndex : forbiddenCycles.keySet()) {
                    addSubtourBreakingConstraint(forbiddenCycles.get(ccIndex), k);
                }
            }
        }

        private int merge(int x) {
            if (connectedComponent[x] == x) {
                return x;
            }
            return merge(connectedComponent[x]);
        }

        private void computeConnectedComponents(int vehicle) throws IloException {
            int N = instance.getNumberOfNodes();
            connectedComponent = new int[N];
            for (int i = 0; i < N; i++) {
                connectedComponent[i] = i;
            }
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (Math.round(this.getValue(x[i][j][vehicle])) == 1) {
                        connectedComponent[merge(i)] = merge(j);
                    }
                }
            }
            for (int i = 0; i < N; i++) {
                connectedComponent[i] = merge(connectedComponent[i]);
            }
        }

        private Map<Integer, Set<Integer>> getForbiddenCycles() {
            Map<Integer, Set<Integer>> forbiddenCycles = new HashMap<>();
            int onlyValidComponent = connectedComponent[instance.getDepot()];
            for (int i = 0; i < instance.getNumberOfNodes(); i++) {
                int ccNumber = connectedComponent[i];
                if (ccNumber != i && ccNumber != onlyValidComponent && !forbiddenCycles.containsKey(ccNumber)) {
                    forbiddenCycles.put(ccNumber, new HashSet<>());
                    for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                        if (connectedComponent[j] == ccNumber) {
                            forbiddenCycles.get(ccNumber).add(j);
                        }
                    }
                }
            }
            return forbiddenCycles;
        }

        private void addSubtourBreakingConstraint(Set<Integer> forbiddenCycle, int k) throws IloException {
            IloLinearIntExpr inCycleEdges = cplex.linearIntExpr();
            for (int i : forbiddenCycle) {
                for (int j : forbiddenCycle) {
                    inCycleEdges.addTerm(x[i][j][k], 1);
                }
            }
            this.add(cplex.le(inCycleEdges, forbiddenCycle.size() - 1));
        }
    }
}

