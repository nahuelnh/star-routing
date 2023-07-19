import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class MasterProblem {

    private final Instance instance;
    private final List<ElementaryPath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerServedConstraints;
    private IloRange numberOfVehiclesConstraint;

    public MasterProblem(Instance instance) {
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
        for (int customer = 0; customer < instance.getNumberOfCustomers(); customer++) {
            IloLinearNumExpr lhs = cplex.linearNumExpr();
            for (int route = 0; route < paths.size(); route++) {
                for (int node = 0; node < instance.getNumberOfNodes(); node++) {
                    int a_isr = paths.get(route).isServedAtNode(node, instance.getCustomer(customer)) ? 1 : 0;
                    lhs.addTerm(theta[route], a_isr);
                }
            }
            customerServedConstraints[customer] = cplex.addEq(lhs, 1);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            lhs.addTerm(theta[i], 1);
        }
        numberOfVehiclesConstraint = cplex.addLe(lhs, instance.getNumberOfVehicles());
    }

    private void createObjective() throws IloException {
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            objective.addTerm(theta[i], paths.get(i).getCost());
        }
        cplex.addMinimize(objective);
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
            Solution solution = new Solution();
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
            IntegerSolution solution = new IntegerSolution();
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public class Solution {
        private final IloCplex.Status status;
        private final double[] visitorDualValues;
        private final double numberOfVehiclesDualValue;


        private Solution() throws IloException {
            this.status = cplex.getStatus();
            if (isFeasible()) {
                this.visitorDualValues = cplex.getDuals(customerServedConstraints);
                this.numberOfVehiclesDualValue = cplex.getDual(numberOfVehiclesConstraint);
            } else {
                this.visitorDualValues = null;
                this.numberOfVehiclesDualValue = 0;
            }
        }

        public boolean isFeasible() {
            return IloCplex.Status.Optimal.equals(this.status) || IloCplex.Status.Feasible.equals(this.status);
        }


        public double getNumberOfVehiclesDualValue() {
            return numberOfVehiclesDualValue;
        }


        public double getVisitorDualValue(int constraintIndex) {
            return visitorDualValues[constraintIndex];
        }
    }

    public class IntegerSolution {

        private final IloCplex.Status status;
        private final double[] primalValues;


        private IntegerSolution() throws IloException {
            this.status = cplex.getStatus();
            this.primalValues = isFeasible() ? cplex.getValues(theta) : null;
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
    }
}
