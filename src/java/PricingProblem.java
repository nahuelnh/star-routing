import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.HashSet;
import java.util.Set;

public class PricingProblem {

    private final Instance instance;
    private final MasterProblem.Solution solution;
    private IloCplex cplex;
    private IloIntVar[][] x;
    private IloIntVar[] z;
    private IloIntVar[][] u;
    private IloIntVar[] v;


    PricingProblem(Instance instance, MasterProblem.Solution solution) {
        this.instance = instance;
        this.solution = solution;
    }


    private void createVariables() throws IloException {
        int N = instance.getNodes();
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
        int N = instance.getNodes();
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

    private void createObjective() throws IloException {
        int N = instance.getNodes();
        int S = instance.getNumberOfCustomers();

        IloLinearIntExpr firstTerm = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                firstTerm.addTerm(u[i][j], instance.getGraphWeights(i, j));
            }
        }
        IloLinearNumExpr secondTerm = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            for (int s = 0; s < S; s++) {
                secondTerm.addTerm(x[i][s], solution.getVisitorDualValues().get(s));
            }
        }
        IloNumExpr objective = cplex.sum(firstTerm, secondTerm);
        objective = cplex.sum(objective, -solution.getNumberOfVehiclesDualValue());
        cplex.addMinimize(objective);
    }

    private Route getRoutesFromSolution() throws IloException {
        int N = instance.getNodes();
        int S = instance.getNumberOfCustomers();

        Route route = Route.emptyRoute();
        int lastNode = instance.getDepot();
        for (int i = 0; i < N; i++) {
            Set<Integer> customersVisited = new HashSet<>();
            for (int s = 0; s < S; s++) {// TODO revisar como hacer igualdad == 1
                if (cplex.getValue(x[i][s]) > 0.5) {
                    customersVisited.add(s);
                }
            }
            if (!customersVisited.isEmpty()) {
                route.addNode(i, customersVisited, instance.getGraphWeights(lastNode, i));
                lastNode = i;
            }
        }
        route.addNode(instance.getDepot(), new HashSet<>(), instance.getGraphWeights(lastNode, instance.getDepot()));

        return route;
    }

    Route solve() throws IloException {
        cplex = new IloCplex();
        createVariables();
        createConstraints();
        createObjective();
        cplex.solve();
        // TODO evitar usar nulls
        Route ret = null;
        if (IloCplex.Status.Optimal.equals(cplex.getStatus()) && cplex.getObjValue() < 0) {
            ret = getRoutesFromSolution();
        }
        cplex.end();
        return ret;
    }
}
