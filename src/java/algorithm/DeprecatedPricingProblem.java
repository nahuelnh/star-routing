package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;
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

public class DeprecatedPricingProblem implements PricingProblem {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private IloCplex cplex;
    private IloIntVar[][] x;
    private IloIntVar[] z;
    private IloIntVar[][] u;
    private IloIntVar[] v;

    public DeprecatedPricingProblem(Instance instance) {
        this.instance = instance;
    }

    private void createVariables() throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        x = new IloIntVar[N][S + 1];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < S + 1; j++) {
                x[i][j] = cplex.boolVar("x_" + i + "_" + j);
            }
        }

        z = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            z[i] = cplex.boolVar("z_" + i);
        }

        u = new IloIntVar[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                u[i][j] = cplex.boolVar("u_" + i + "_" + j);
            }
        }

        v = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            v[i] = cplex.intVar(1, N - 1, "v_" + i);
        }
    }

    private void createConstraints() throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();
        int SExtended = instance.getNumberOfCustomers() + 1;
        int s0 = instance.getNumberOfCustomers();

        // First constraint
        for (int i = 0; i < N; i++) {
            IloIntExpr lhs = Utils.getArraySum(cplex, x[i]);
            IloIntExpr rhs = cplex.prod(z[i], SExtended);
            cplex.addLe(lhs, rhs, "z_consistent_" + i);
        }

        // Second constraint
        for (int s = 0; s < S; s++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int i = 0; i < N; i++) {
                lhs.addTerm(x[i][s], 1);
            }
            cplex.addLe(lhs, 1, "serving_" + s);
        }

        // Third Constraint
        cplex.addEq(z[instance.getDepot()], 1, "depot");

        // Fourth Constraint
        for (int s = 0; s < S; s++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int i = 0; i < N; i++) {
                if (!instance.getNeighbors(instance.getCustomer(s)).contains(i)) {
                    lhs.addTerm(x[i][s], 1);
                }
            }
            cplex.addEq(lhs, 0, "neighbor_" + s);
        }

        // Fifth constraint
        IloLinearIntExpr lhs = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int s = 0; s < S; s++) {
                lhs.addTerm(x[i][s], instance.getDemand(instance.getCustomer(s)));
            }
        }
        cplex.addLe(lhs, instance.getCapacity(), "capacity");

        // Sixth Constraint
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            IloIntExpr numberOfCustomersServed = Utils.getArraySum(cplex, x[i]);
            IloIntExpr rhs = cplex.sum(SExtended, cplex.prod(-SExtended, x[i][s0]));
            cplex.addLe(numberOfCustomersServed, rhs, "non_existing_customer_" + i);
        }

        // Seventh constraint
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != j) {
                    cplex.addLe(cplex.sum(u[i][j], 1), cplex.sum(z[i], z[j]), "u_consistent_" + i + "_" + j);
                }
            }
            cplex.addEq(u[i][i], 0);
        }

        // Eighth constraint
        for (int i = 0; i < N; i++) {
            IloLinearIntExpr sum1 = cplex.linearIntExpr();
            IloLinearIntExpr sum2 = cplex.linearIntExpr();
            for (int j = 0; j < N; j++) {
                sum1.addTerm(u[i][j], 1);
                sum2.addTerm(u[j][i], 1);
            }
            cplex.addEq(z[i], sum1, "inflow_" + i);
            cplex.addEq(z[i], sum2, "outflow_" + i);
        }

        // MTZ constraints
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != instance.getDepot() && j != instance.getDepot() && j != i) {
                    IloIntExpr mtz = cplex.sum(v[i], cplex.negative(v[j]), cplex.prod(u[i][j], N - 1));
                    cplex.addLe(mtz, N - 2, "mtz_" + i + "_" + j);
                }
            }
        }
    }

    private void createObjective(RestrictedMasterProblem.RMPSolution rmpSolution) throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        IloLinearIntExpr firstTerm = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != j) {
                    firstTerm.addTerm(u[i][j], instance.getEdgeWeight(i, j));
                }
            }
        }
        IloLinearNumExpr secondTerm = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            for (int s = 0; s < S; s++) {
                secondTerm.addTerm(x[i][s], rmpSolution.getCustomerDual(s));
            }
        }
        IloNumExpr objective = cplex.sum(firstTerm, cplex.negative(secondTerm));
        objective = cplex.sum(objective, -rmpSolution.getVehiclesDual());
        cplex.addMinimize(objective);
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            createVariables();
            createConstraints();
            createObjective(rmpSolution);
            cplex.solve();
            Solution solution = new Solution(cplex.getStatus(), cplex.getObjValue(), this);
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        try {
            List<FeasiblePath> ret = new ArrayList<>();
            if (!Utils.isSolutionFeasible(cplex)) {
                return ret;
            }
            for (int solutionIndex = 0; solutionIndex < cplex.getSolnPoolNsolns(); solutionIndex++) {
                if (cplex.getObjValue(solutionIndex) < -EPSILON) {
                    ret.add(getPathFromFeasibleSolution(solutionIndex));
                }
            }
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private int getNextNodeInPath(int from, int solutionIndex) throws IloException {
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (Utils.getBoolValue(cplex, u[from][i], solutionIndex)) {
                return i;
            }
        }
        throw new AssertionError(String.format("Path starting in %d has no end", from));
    }

    private FeasiblePath getPathFromFeasibleSolution(int solutionIndex) throws IloException {
        FeasiblePath feasiblePath = new FeasiblePath();
        int lastNode = instance.getDepot();
        int currentNode = getNextNodeInPath(lastNode, solutionIndex);
        while (currentNode != instance.getDepot()) {
            Set<Integer> customersVisited = new HashSet<>();
            for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                if (Utils.getBoolValue(cplex, x[currentNode][s], solutionIndex)) {
                    customersVisited.add(instance.getCustomer(s));
                }
            }
            feasiblePath.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
            feasiblePath.addCustomers(customersVisited);
            lastNode = currentNode;
            currentNode = getNextNodeInPath(currentNode, solutionIndex);
        }
        feasiblePath.addNode(instance.getDepot(), instance.getEdgeWeight(lastNode, instance.getDepot()));
        return feasiblePath;
    }

}
