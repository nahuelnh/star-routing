import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MasterProblem {

    private final Instance instance;
    private final List<Route> routes;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] visitorConstraints;
    private IloRange numberOfVehiclesConstraint;

    public MasterProblem(Instance instance, List<Route> routes) throws IloException {
        this.instance = instance;
        this.routes = routes;
    }


    private void addConstraintRow(int customer) throws IloException {
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for (int route = 0; route < routes.size(); route++) {
            for (int node = 0; node < instance.getNumberOfNodes(); node++) {
                int a_isr = routes.get(route).isServedAtNode(node, instance.getCustomer(customer)) ? 1 : 0;
                lhs.addTerm(theta[route], a_isr);
            }
        }
        visitorConstraints[customer] = cplex.addEq(lhs, 1);
    }

    private void buildModel(boolean integral) throws IloException {
        cplex = new IloCplex();
        int N = routes.size();
        int S = instance.getNumberOfCustomers();

        // Variable declaration
        theta = new IloNumVar[N];
        for (int i = 0; i < N; i++) {
            theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
        }

        // Add visitor constraints
        // Every customer is visited and served exactly once
        visitorConstraints = new IloRange[S];
        for (int customer = 0; customer < S; customer++) {
            addConstraintRow(customer);
        }

        // Add constraint on number of vehicles used
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            lhs.addTerm(theta[i], 1);
        }
        numberOfVehiclesConstraint = cplex.addEq(lhs, instance.getNumberOfVehicles());

        // Add objective function
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < N; i++) {
            objective.addTerm(theta[i], routes.get(i).getCost());
        }
        cplex.addMinimize(objective);
    }

    private Solution solve(boolean integral) throws IloException {
        buildModel(integral);
        cplex.solve();
        Solution solution = new Solution(cplex.getStatus(),
                cplex.getValues(theta),
                cplex.getDuals(visitorConstraints),
                cplex.getDual(numberOfVehiclesConstraint),
                cplex.getObjValue()
        );
        cplex.end();
        return solution;
    }

    public Solution solveRelaxation() throws IloException {
        return solve(false);
    }

    public Solution solveInteger() throws IloException {
        return solve(true);
    }

    public static class Solution {
        private final IloCplex.Status status;
        private final List<Double> primalValues;
        private final List<Double> visitorDualValues;
        private final double numberOfVehiclesDualValue;
        private final double objectiveValue;

        public Solution(IloCplex.Status status, double[] primalValues, double[] visitorDualValues, double numberOfVehiclesDualValue, double objectiveValue) {
            this.status = status;
            this.primalValues = Arrays.stream(primalValues).boxed().collect(Collectors.toList());
            this.visitorDualValues = Arrays.stream(visitorDualValues).boxed().collect(Collectors.toList());
            this.numberOfVehiclesDualValue = numberOfVehiclesDualValue;
            this.objectiveValue = objectiveValue;
        }

        public IloCplex.Status getStatus() {
            return status;
        }

        public double getNumberOfVehiclesDualValue() {
            return numberOfVehiclesDualValue;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }

        public List<Double> getPrimalValues() {
            return primalValues;
        }

        public List<Double> getVisitorDualValues() {
            return visitorDualValues;
        }
    }


}
