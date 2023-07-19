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

public class PricingProblem {

    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private IloCplex cplex;
    private IloIntVar[][] x;
    private IloIntVar[] z;
    private IloIntVar[][] u;
    private IloIntVar[] v;

    PricingProblem(Instance instance) {
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
            IloIntExpr lhs = Utils.getIntArraySum(cplex, x[i]);
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
            IloIntExpr numberOfCustomersServed = Utils.getIntArraySum(cplex, x[i]);
            IloIntExpr rhs = cplex.sum(SExtended, cplex.prod(-SExtended, x[i][s0]));
            cplex.addLe(numberOfCustomersServed, rhs, "non_existing_customer_" + i);
        }

        // Seventh constraint
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cplex.addLe(cplex.sum(u[i][j], 1), cplex.sum(z[i], z[j]), "u_consistent_" + i + "_" + j);
            }
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

    private void createObjective(RestrictedMasterProblem.Solution rmpSolution) throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        IloLinearIntExpr firstTerm = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                firstTerm.addTerm(u[i][j], instance.getEdgeWeight(i, j));
            }
        }
        IloLinearNumExpr secondTerm = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            for (int s = 0; s < S; s++) {
                secondTerm.addTerm(x[i][s], rmpSolution.getVisitorDualValue(s));
            }
        }
        IloNumExpr objective = cplex.diff(firstTerm, secondTerm);
        objective = cplex.diff(objective, rmpSolution.getNumberOfVehiclesDualValue());
        cplex.addMinimize(objective);
    }

    Solution solve(RestrictedMasterProblem.Solution rmpSolution) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            createVariables();
            createConstraints();
            createObjective(rmpSolution);
            cplex.solve();
            Solution solution = new Solution();
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public class Solution {
        private final IloCplex.Status status;
        private final double objectiveValue;
        private final List<ElementaryPath> negativeReducedCostPaths;

        Solution() throws IloException {
            this(cplex.getStatus(), cplex.getObjValue());
        }

        Solution(IloCplex.Status status, double objectiveValue) throws IloException {
            this.status = status;
            this.objectiveValue = objectiveValue;
            this.negativeReducedCostPaths = getRoutesFromSolution();
        }

        private boolean hasNegativeReducedCostPaths() {
            return (IloCplex.Status.Optimal.equals(status) || IloCplex.Status.Feasible.equals(status)) &&
                    objectiveValue < -EPSILON;
        }

        private ElementaryPath getPathFromFeasibleSolution(int solutionIndex) throws IloException {
            int s0 = instance.getNumberOfCustomers();
            ElementaryPath elementaryPath = ElementaryPath.emptyPath();
            int lastNode = instance.getDepot();
            for (int i = 0; i < instance.getNumberOfNodes(); i++) {
                Set<Integer> customersVisited = new HashSet<>();
                for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                    if (Math.round(cplex.getValue(x[i][s], solutionIndex)) == 1) {
                        customersVisited.add(s);
                    }
                }
                if (!customersVisited.isEmpty() || Math.round(cplex.getValue(x[i][s0], solutionIndex)) == 1) {
                    elementaryPath.addNode(i, customersVisited, instance.getEdgeWeight(lastNode, i));
                    lastNode = i;
                }
            }
            elementaryPath.addNode(instance.getDepot(), new HashSet<>(),
                    instance.getEdgeWeight(lastNode, instance.getDepot()));
            return elementaryPath;
        }

        private List<ElementaryPath> getRoutesFromSolution() throws IloException {
            List<ElementaryPath> ret = new ArrayList<>();
            if (!hasNegativeReducedCostPaths()) {
                return ret;
            }
            for (int solutionIndex = 0; solutionIndex < cplex.getSolnPoolNsolns(); solutionIndex++) {
                if (cplex.getObjValue(solutionIndex) < -EPSILON) {
                    ret.add(getPathFromFeasibleSolution(solutionIndex));
                }
            }
            return ret;
        }

        public List<ElementaryPath> getNegativeReducedCostPaths() {
            return negativeReducedCostPaths;
        }
    }
}
