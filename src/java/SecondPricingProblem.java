import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class SecondPricingProblem implements PricingProblem {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private IloCplex cplex;
    private IloIntVar[][] x;
    private IloIntVar[] y;
    private IloIntVar[] u;

    private IloIntExpr[] visitConstraint;

    public SecondPricingProblem(Instance instance) {
        this.instance = instance;
    }

    private void createVariables() throws IloException {
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
            u[i] = cplex.boolVar("u_" + i);
        }
    }

    private void createFlowConstraints() throws IloException {
        // Vehicle leaves the node that it enters
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            IloLinearIntExpr inFlow = cplex.linearIntExpr();
            IloLinearIntExpr outFlow = cplex.linearIntExpr();
            for (int j = 0; j < instance.getNumberOfNodes(); j++) {
                inFlow.addTerm(x[i][j], 1);
                outFlow.addTerm(x[j][i], 1);
            }
            cplex.addEq(inFlow, outFlow, "flow_" + i);
        }
    }

    private void createDepotConstraints() throws IloException {
        // Every vehicle leaves the depot
        IloIntExpr outgoingEdgesFromDepot = Utils.getIntArraySum(cplex, x[instance.getDepot()]);
        cplex.addEq(outgoingEdgesFromDepot, 1, "depot");
    }

    private void createVisitConstraints() throws IloException {
        // A vehicle can only serve visited customers
        visitConstraint = new IloIntExpr[instance.getNumberOfCustomers()];
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            int currentCustomer = instance.getCustomer(s);
            IloIntExpr rhs = cplex.linearIntExpr();
            for (int i = 0; i < instance.getNumberOfNodes(); i++) {
                for (int neighbor : instance.getNeighbors(currentCustomer)) {
                    rhs = cplex.sum(rhs, x[i][neighbor]);
                }
            }
            visitConstraint[s] = rhs;
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

    private void createConstraints() throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            cplex.addEq(x[i][i], 0);
        }
        createFlowConstraints();
        createDepotConstraints();
        createVisitConstraints();
        createCapacityConstraints();
        createMTZConstraints();
    }

    private void createObjective(RestrictedMasterProblem.Solution rmpSolution) throws IloException {
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
            secondTerm.addTerm(y[s], rmpSolution.getVisitorDualValue(s));
        }

        IloNumExpr objective = cplex.sum(firstTerm, cplex.negative(secondTerm));
        objective = cplex.sum(objective, -rmpSolution.getNumberOfVehiclesDualValue());
        cplex.addMinimize(objective);
    }

    @Override
    public PricingProblem.Solution solve(RestrictedMasterProblem.Solution rmpSolution) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            createVariables();
            createConstraints();
            createObjective(rmpSolution);
            cplex.solve();
            PricingProblem.Solution solution = new PricingProblem.Solution(cplex, getRoutesFromSolution());

//            for (int i = 0; i < instance.getNumberOfNodes(); i++) {
//                for (int j = 0; j < instance.getNumberOfNodes(); j++) {
//                    if (Math.round(cplex.getValue(x[i][j])) == 1) {
//                        System.out.println(x[i][j]);
//                    }
//                }
//            }
//
//            System.out.println("Obj: " + cplex.getObjValue());
//            int reducedCost = getPathFromFeasibleSolution(0).getCost();
//            for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
//                if (Math.round(cplex.getValue(y[s])) == 1) {
//                    System.out.println(visitConstraint[s]);
//                    System.out.println(y[s] + " " + instance.getCustomer(s) + " " +
//                            instance.getNeighbors(instance.getCustomer(s)));
//                }
//                reducedCost -= rmpSolution.getVisitorDualValue(s) * Math.round(cplex.getValue(y[s]));
//            }
//            reducedCost -= rmpSolution.getNumberOfVehiclesDualValue();
//            System.out.println("RC: " + reducedCost);
//            System.out.println("Cost: " + getPathFromFeasibleSolution(0).getCost() + " dual: " +
//                    rmpSolution.getNumberOfVehiclesDualValue() + " duals: " +
//                    IntStream.range(0, instance.getNumberOfCustomers()).mapToObj(rmpSolution::getVisitorDualValue)
//                            .toList());
//            System.out.println();

            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isFeasible() throws IloException {
        return IloCplex.Status.Optimal.equals(cplex.getStatus()) || IloCplex.Status.Feasible.equals(cplex.getStatus());
    }

    private int getNextNodeInPath(int from, int solutionIndex) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (Math.round(cplex.getValue(x[from][i], solutionIndex)) == 1) {
                return i;
            }
        }
        throw new AssertionError(String.format("Path starting in %d has no end", from));
    }

    private Set<Integer> getCustomersServed(int currentNode, int solutionIndex, Set<Integer> alreadyVisited)
            throws IloException {
        Set<Integer> customersServed = new HashSet<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            int customer = instance.getCustomer(s);
            if (!alreadyVisited.contains(customer) && instance.getNeighbors(customer).contains(currentNode) &&
                    Math.round(cplex.getValue(y[s], solutionIndex)) == 1) {
                customersServed.add(customer);
                alreadyVisited.add(customer);
            }
        }
        return customersServed;
    }

    private ElementaryPath getPathFromFeasibleSolution(int solutionIndex) throws IloException {
        ElementaryPath path = ElementaryPath.emptyPath();
        Set<Integer> alreadyVisited = new HashSet<>();
        int lastNode = instance.getDepot();
        int currentNode = getNextNodeInPath(lastNode, solutionIndex);
        while (currentNode != instance.getDepot()) {
            path.addNode(currentNode, getCustomersServed(currentNode, solutionIndex, alreadyVisited),
                    instance.getEdgeWeight(lastNode, currentNode));
            lastNode = currentNode;
            currentNode = getNextNodeInPath(currentNode, solutionIndex);
        }
        path.addNode(currentNode, getCustomersServed(currentNode, solutionIndex, alreadyVisited),
                instance.getEdgeWeight(lastNode, currentNode));
        return path;
    }

    private List<ElementaryPath> getRoutesFromSolution() throws IloException {
        List<ElementaryPath> ret = new ArrayList<>();
        if (!isFeasible()) {
            return ret;
        }
        for (int solutionIndex = 0; solutionIndex < cplex.getSolnPoolNsolns(); solutionIndex++) {
            if (cplex.getObjValue(solutionIndex) < -EPSILON) {
                ret.add(getPathFromFeasibleSolution(solutionIndex));
            }
        }
        return ret;
    }
}
