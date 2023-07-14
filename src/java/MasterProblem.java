import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class MasterProblem {

    private final Instance instance;
    private final List<Route> routes;

    MasterProblem(Instance instance, List<Route> routes) {
        this.instance = instance;
        this.routes = routes;
    }

    void solve() throws IloException {
        int N = routes.size();
        int S = instance.getCustomers().size();
        IloCplex cplex = new IloCplex();

        IloIntVar[] theta = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            theta[i] = cplex.boolVar("theta_" + i);
        }

        IloRange[] visitorConstraints = new IloRange[S];

        for (int customer = 0; customer < S; customer++) {
            IloLinearIntExpr lhs = cplex.linearIntExpr();
            for (int route = 0; route < N; route++) {
                for (int node = 0; node < instance.getNodes(); node++) {
                    int a_isr = routes.get(route).inRoute(node, customer) ? 1 : 0;
                    lhs.addTerm(theta[route], a_isr);
                }
            }
            visitorConstraints[customer] = cplex.addLe(lhs, 1);
        }
        IloLinearIntExpr lhs = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            lhs.addTerm(theta[i], 1);
        }
        IloRange numberOfRoutesConstraints = cplex.addLe(lhs, instance.getVehicles());
        IloLinearIntExpr objective = cplex.linearIntExpr();
        for (int i = 0; i < N; i++) {
            int cost = 0;
            objective.addTerm(theta[i], cost);
        }
        cplex.addMinimize(objective);

        cplex.solve();
        System.out.println(cplex.getStatus());
        cplex.end();
    }

    void solveInteger() {

    }

    List<Route> getSolution() {
        return new ArrayList<>();
    }
}
