package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnFleetSize;
import algorithm.branching.BranchOnVisitFlow;
import commons.Route;
import commons.Instance;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ILPPricingProblem extends PricingProblem {

    private static final double EPSILON = 1.0e-6;

    private final Instance instance;

    private IloCplex      cplex;
    private IloIntVar[][] x;
    private IloIntVar[]   y;
    private IloIntVar[]   u;
    private IloIntVar[]   z;

    public ILPPricingProblem(Instance instance) {
        this.instance = instance;
    }

    private void createVariables(RMPLinearSolution rmpSolution) throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        x = new IloIntVar[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                x[i][j] = cplex.boolVar("x_" + i + "_" + j);
            }
        }

        y = new IloIntVar[S];
        for (int s = 0; s < S; s++) {
            y[s] = cplex.boolVar("y_" + s);
        }

        u = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            u[i] = cplex.intVar(0, N - 1, "u_" + i);
        }

        z = new IloIntVar[rmpSolution.getVisitFlowDuals().size()];
        for (int i = 0; i < rmpSolution.getVisitFlowDuals().size(); i++) {
            z[i] = cplex.boolVar("z_" + i);
        }
    }

    private void createFlowConstraints() throws IloException {
        // Vehicle leaves the node that it enters
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            IloIntExpr inFlow  = Utils.getRowSum(cplex, x, i);
            IloIntExpr outFlow = Utils.getColumnSum(cplex, x, i);
            cplex.addEq(inFlow, outFlow, "flow_" + i);
        }
    }

    private void createDepotConstraints() throws IloException {
        // Every vehicle leaves the depot
        IloIntExpr outgoingEdgesFromDepot = Utils.getRowSum(cplex, x, instance.getDepot());
        cplex.addEq(outgoingEdgesFromDepot, 1, "depot");
    }

    private void createVisitConstraints() throws IloException {
        // A vehicle can only serve visited customers
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            int        currentCustomer = instance.getCustomer(s);
            IloIntExpr rhs             = cplex.linearIntExpr();
            for (int i = 0; i < instance.getNumberOfNodes(); i++) {
                for (int neighbor : instance.getNeighbors(currentCustomer)) {
                    rhs = cplex.sum(rhs, x[i][neighbor]);
                }
            }
            cplex.addLe(y[s], rhs, "visit_" + s);
        }
    }

    private void createCapacityConstraints() throws IloException {
        // Capacity constraint
        IloLinearIntExpr totalDemand = cplex.linearIntExpr();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            totalDemand.addTerm(y[s], instance.getDemand(instance.getCustomer(s)));
        }
        cplex.addLe(totalDemand, instance.getCapacity(), "capacity");
    }

    private void createMTZConstraints() throws IloException {
        // Subtour Elimination Constraints (SEC)
        int N = instance.getNumberOfNodes();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != instance.getDepot() && j != instance.getDepot() && j != i) {
                    IloIntExpr mtz = cplex.sum(u[i], cplex.negative(u[j]), cplex.prod(x[i][j], N - 1));
                    cplex.addLe(mtz, N - 2, "mtz_" + i + "_" + j);
                }
            }
        }
    }

    private void createBranchingConstraints(RMPLinearSolution rmpSolution) throws IloException {
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            if (branch.isLowerBound() && branch.getBound() == 1) {
                cplex.addLe(y[branch.getCustomer()], x[branch.getEdge().getStart()][branch.getEdge().getEnd()]);
            } else if (branch.isUpperBound() && branch.getBound() == 0) {
                cplex.addLe(x[branch.getEdge().getStart()][branch.getEdge().getEnd()], y[branch.getCustomer()]);
            }
        }
    }

    private void createVisitFlowConstraints(RMPLinearSolution rmpSolution) throws IloException {
        int branchIndex = 0;
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            int i = branch.getEdge().getStart();
            int j = branch.getEdge().getEnd();
            cplex.addLe(z[branchIndex], y[branch.getCustomer()]);
            cplex.addLe(z[branchIndex], x[i][j]);
            cplex.addGe(cplex.sum(z[branchIndex], 1), cplex.sum(y[branch.getCustomer()], x[i][j]));
            branchIndex++;
        }
    }

    private void createConstraints(RMPLinearSolution rmpSolution) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            cplex.addEq(x[i][i], 0);
        }
        createFlowConstraints();
        createDepotConstraints();
        createVisitConstraints();
        createCapacityConstraints();
        createBranchingConstraints(rmpSolution);
        createVisitFlowConstraints(rmpSolution);
        createMTZConstraints();
    }

    private void createObjective(RMPLinearSolution rmpSolution) throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        IloLinearIntExpr firstTerm = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != j) {
                    firstTerm.addTerm(x[i][j], instance.getEdgeWeight(i, j));
                }
            }
        }
        IloLinearNumExpr secondTerm = cplex.linearNumExpr();
        for (int s = 0; s < S; s++) {
            secondTerm.addTerm(y[s], rmpSolution.getCustomerDual(s));
        }
        IloLinearNumExpr thirdTerm   = cplex.linearNumExpr();
        int              branchIndex = 0;
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            double dual = rmpSolution.getVisitFlowDuals().get(branch);
            thirdTerm.addTerm(z[branchIndex], dual);
            branchIndex++;
        }

        IloNumExpr objective = cplex.sum(firstTerm, cplex.negative(secondTerm));
        objective = cplex.sum(objective, cplex.negative(thirdTerm));
        objective = cplex.sum(objective, getInitialCost(rmpSolution));
        cplex.addMinimize(objective);
    }

    @Override
    public PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            cplex.setParam(IloCplex.Param.TimeLimit, remainingTime.getSeconds() + 1);
            createVariables(rmpSolution);
            createConstraints(rmpSolution);
            createObjective(rmpSolution);

            performBranching();

            cplex.solve();
            boolean     feasible          = IloCplex.Status.Optimal.equals(cplex.getStatus());
            List<Route> pathsFromSolution = feasible ? computePathsFromSolution() : new ArrayList<>();
            PricingSolution pricingSolution =
                new PricingSolution(cplex.getObjValue(), pathsFromSolution, cplex.getDetTime(), feasible);
            cplex.end();
            return pricingSolution;
        } catch (IloException e) {
            cplex.end();
            return new PricingSolution();
        }
    }

    private int getNextNodeInPath(int from, int solutionIndex) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (Utils.getBoolValue(cplex, x[from][i], solutionIndex)) {
                return i;
            }
        }
        throw new AssertionError(String.format("Path starting in %d has no end", from));
    }

    private Set<Integer> getVisitedCustomers(int solutionIndex) throws IloException {
        Set<Integer> visitedCustomers = new HashSet<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            if (Utils.getBoolValue(cplex, y[s], solutionIndex)) {
                visitedCustomers.add(instance.getCustomer(s));
            }
        }
        return visitedCustomers;
    }

    private Route getPathFromFeasibleSolutionInPool(int solutionIndex) throws IloException {
        Route path        = new Route();
        int   lastNode    = instance.getDepot();
        int   currentNode = getNextNodeInPath(lastNode, solutionIndex);
        while (currentNode != instance.getDepot()) {
            path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
            lastNode    = currentNode;
            currentNode = getNextNodeInPath(currentNode, solutionIndex);
        }
        path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
        path.addCustomers(getVisitedCustomers(solutionIndex));
        return path;
    }

    private double getInitialCost(RMPLinearSolution rmpSolution) {
        double initialCost = -rmpSolution.getVehiclesDual();
        for (double fleetSizeDual : rmpSolution.getFleetSizeDuals()) {
            initialCost -= fleetSizeDual;
        }
        return initialCost;
    }

    public List<Route> computePathsFromSolution() {
        try {
            List<Route> ret = new ArrayList<>();
            if (!Utils.isSolutionFeasible(cplex)) {
                return ret;
            }
            for (int solutionIndex = 0; solutionIndex < cplex.getSolnPoolNsolns(); solutionIndex++) {
                if (cplex.getObjValue(solutionIndex) < -EPSILON) {
                    ret.add(getPathFromFeasibleSolutionInPool(solutionIndex));
                }
            }
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forceExactSolution() {
    }

    @Override
    public void performBranchOnVisitFlow(BranchOnVisitFlow branch) {

    }

    @Override
    public void performBranchOnFleetSize(BranchOnFleetSize branch) {

    }
}
