import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class RestrictedMasterProblem {

    private final Instance instance;
    private final List<ElementaryPath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerServedConstraints;
    private IloRange numberOfVehiclesConstraint;

    public RestrictedMasterProblem(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    public void addPaths(List<ElementaryPath> newPaths) {
        paths.addAll(newPaths);
    }

    private void createVariables(boolean integral) throws IloException {
        theta = new IloNumVar[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
        }
    }

    private void createCustomerServedConstraints() throws IloException {
        customerServedConstraints = new IloRange[instance.getNumberOfCustomers()];
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = cplex.linearNumExpr();
            int customer = instance.getCustomer(s);
            for (int route = 0; route < paths.size(); route++) {
                if (paths.get(route).isCustomerServed(customer)) {
                    lhs = cplex.sum(lhs, theta[route]);
                }
            }
            customerServedConstraints[s] = cplex.addEq(lhs, 1, "customer_served_" + s);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloNumExpr lhs = Utils.getNumArraySum(cplex, theta);
        if (instance.unusedVehiclesAllowed()) {
            numberOfVehiclesConstraint = cplex.addLe(lhs, instance.getNumberOfVehicles(), "number_vehicles");
        } else {
            numberOfVehiclesConstraint = cplex.addEq(lhs, instance.getNumberOfVehicles(), "number_vehicles");
        }
    }

    private void createObjective() throws IloException {
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            objective.addTerm(theta[i], paths.get(i).getCost());
        }
        cplex.addMinimize(objective, "cost");
    }

    private void buildModel(boolean integral) throws IloException {
        cplex = new IloCplex();
        cplex.setOut(null);
        createVariables(integral);
        createCustomerServedConstraints();
        createNumberOfVehiclesConstraint();
        createObjective();
    }

    public Solution solveRelaxation() {
        try {
            buildModel(false);
            cplex.solve();
            Solution solution = new Solution(cplex, customerServedConstraints, numberOfVehiclesConstraint);
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public IntegerSolution solveInteger() {
        try {
            buildModel(true);
            cplex.solve();
            IntegerSolution solution = new IntegerSolution(cplex, theta, paths);
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Solution {
        private final IloCplex.Status status;
        private final double objectiveValue;
        private final double[] visitorDualValues;
        private final double numberOfVehiclesDualValue;

        private Solution(IloCplex cplex, IloRange[] customerServedConstraints, IloRange numberOfVehiclesConstraint)
                throws IloException {
            this.status = cplex.getStatus();
            if (!isFeasible()) {
                throw new IllegalStateException("Restricted Master Problem is not feasible");
            }
            this.objectiveValue = cplex.getObjValue();
            this.visitorDualValues = cplex.getDuals(customerServedConstraints);
            this.numberOfVehiclesDualValue = cplex.getDual(numberOfVehiclesConstraint);
        }

        public boolean isFeasible() {
            return IloCplex.Status.Optimal.equals(status) || IloCplex.Status.Feasible.equals(status);
        }

        public double getNumberOfVehiclesDualValue() {
            return numberOfVehiclesDualValue;
        }

        public double getVisitorDualValue(int constraintIndex) {
            return visitorDualValues[constraintIndex];
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }

    public static class IntegerSolution {

        private final IloCplex.Status status;
        private final double objectiveValue;
        private final double[] primalValues;
        private final List<ElementaryPath> paths;

        private IntegerSolution(IloCplex cplex, IloNumVar[] theta, List<ElementaryPath> paths) throws IloException {
            this.status = cplex.getStatus();
            this.objectiveValue = cplex.getObjValue();
            this.primalValues = isFeasible() ? cplex.getValues(theta) : null;
            this.paths = paths;
        }

        public boolean isFeasible() {
            return IloCplex.Status.Optimal.equals(status) || IloCplex.Status.Feasible.equals(status);
        }

        public List<ElementaryPath> getUsedPaths() {
            List<ElementaryPath> ret = new ArrayList<>();
            if (!isFeasible()) {
                return ret;
            }
            for (int i = 0; i < paths.size(); i++) {
                if (Math.round(primalValues[i]) == 1) {
                    ret.add(paths.get(i));
                }
            }
            return ret;
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }
    }
}
