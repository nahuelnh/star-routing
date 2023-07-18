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

    private static final double EPSILON = 1e6;
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

        x = new IloIntVar[N][S];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < S; j++) {
                x[i][j] = cplex.boolVar();
            }
        }

        z = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            z[i] = cplex.boolVar();
        }

        u = new IloIntVar[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                u[i][j] = cplex.boolVar();
            }
        }

        v = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            v[i] = cplex.intVar(1, N - 1);
        }
    }

    private void createConstraints() throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();


        // First constraint
        for (int i = 0; i < N; i++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int s = 0; s < S; s++) {
                lhs.addTerm(x[i][s], 1);
            }
            IloIntExpr rhs = cplex.prod(z[i], S);
            cplex.addLe(lhs, rhs);
        }

        // Second constraint
        for (int s = 0; s < S; s++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int i = 0; i < N; i++) {
                lhs.addTerm(x[i][s], 1);
            }
            cplex.addLe(lhs, 1);
        }

        // Third Constraint
        cplex.addEq(z[instance.getDepot()], 1);

        // Fourth Constraint
        for (int s = 0; s < S; s++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int i = 0; i < N; i++) {
                if (!instance.getNeighbors(instance.getCustomer(s)).contains(i)) {
                    lhs.addTerm(x[i][s], 1);
                }
            }
            cplex.addEq(lhs, 0);
        }

        // Fifth constraint
        IloLinearIntExpr lhs = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int s = 0; s < S; s++) {
                lhs.addTerm(x[i][s], instance.getDemand(instance.getCustomer(s)));
            }
        }
        cplex.addLe(lhs, instance.getCapacity());

        //Seventh constraint
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cplex.addLe(cplex.sum(u[i][j], 1), cplex.sum(z[i], z[j]));
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
            cplex.addEq(z[i], sum1);
            cplex.addEq(z[i], sum2);
        }

        // MTZ constraints
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i != instance.getDepot() && j != instance.getDepot() && j != i) {
                    IloIntExpr mtz = cplex.sum(v[i], cplex.negative(v[j]), cplex.prod(u[i][j], N - 1));
                    cplex.addLe(mtz, N - 2);
                }
            }
        }
    }

    private void createObjective(MasterProblem.Solution rmpSolution) throws IloException {
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
                secondTerm.addTerm(x[i][s], rmpSolution.getVisitorDualValues().get(s));
            }
        }
        IloNumExpr objective = cplex.sum(firstTerm, secondTerm);
        objective = cplex.sum(objective, -rmpSolution.getNumberOfVehiclesDualValue());
        cplex.addMinimize(objective);
    }

    private ElementaryPath getRoutesFromSolution() throws IloException {
        int N = instance.getNumberOfNodes();
        int S = instance.getNumberOfCustomers();

        ElementaryPath elementaryPath = ElementaryPath.emptyPath();
        int lastNode = instance.getDepot();
        for (int i = 0; i < N; i++) {
            Set<Integer> customersVisited = new HashSet<>();
            for (int s = 0; s < S; s++) {// TODO revisar como hacer igualdad == 1
                if (cplex.getValue(x[i][s]) > 0.5) {
                    customersVisited.add(s);
                }
            }
            if (!customersVisited.isEmpty()) {
                elementaryPath.addNode(i, customersVisited, instance.getEdgeWeight(lastNode, i));
                lastNode = i;
            }
        }
        elementaryPath.addNode(instance.getDepot(), new HashSet<>(), instance.getEdgeWeight(lastNode, instance.getDepot()));

        return elementaryPath;
    }

    Solution solve(MasterProblem.Solution rmpSolution) {
        try {
            cplex = new IloCplex();
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
            return (IloCplex.Status.Optimal.equals(status) || IloCplex.Status.Feasible.equals(status)) && objectiveValue < EPSILON;
        }

        private List<ElementaryPath> getRoutesFromSolution() throws IloException {
            List<ElementaryPath> ret = new ArrayList<>();
            if (!hasNegativeReducedCostPaths()) {
                return ret;
            }
            for (int solIdx = 0; solIdx < cplex.getSolnPoolNsolns(); solIdx++) {
                if (cplex.getObjValue(solIdx) < -EPSILON) {
                    ElementaryPath elementaryPath = ElementaryPath.emptyPath();
                    int lastNode = instance.getDepot();
                    for (int i = 0; i < instance.getNumberOfNodes(); i++) {
                        Set<Integer> customersVisited = new HashSet<>();
                        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                            if (Math.round(cplex.getValue(x[i][s], solIdx)) == 1) {
                                customersVisited.add(s);
                            }
                        }
                        if (!customersVisited.isEmpty()) {
                            elementaryPath.addNode(i, customersVisited, instance.getEdgeWeight(lastNode, i));
                            lastNode = i;
                        }
                    }
                    elementaryPath.addNode(instance.getDepot(), new HashSet<>(), instance.getEdgeWeight(lastNode, instance.getDepot()));
                }
            }
            return ret;
        }

        public List<ElementaryPath> getNegativeReducedCostPaths() {
            return negativeReducedCostPaths;
        }
    }
}
