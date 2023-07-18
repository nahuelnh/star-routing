import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MasterProblem {

    private final Instance instance;
    List<ElementaryPath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] visitorConstraints;
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
        visitorConstraints = new IloRange[instance.getNumberOfCustomers()];
        for (int customer = 0; customer < instance.getNumberOfCustomers(); customer++) {
            IloLinearNumExpr lhs = cplex.linearNumExpr();
            for (int route = 0; route < paths.size(); route++) {
                for (int node = 0; node < instance.getNumberOfNodes(); node++) {
                    int a_isr = paths.get(route).isServedAtNode(node, instance.getCustomer(customer)) ? 1 : 0;
                    lhs.addTerm(theta[route], a_isr);
                }
            }
            visitorConstraints[customer] = cplex.addEq(lhs, 1);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            lhs.addTerm(theta[i], 1);
        }
        numberOfVehiclesConstraint = cplex.addEq(lhs, instance.getNumberOfVehicles());
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
        private final List<Double> primalValues;
        private final List<Double> visitorDualValues;
        private final double numberOfVehiclesDualValue;
        private final double objectiveValue;

        private Solution() throws IloException {
            this(cplex.getStatus(), cplex.getValues(theta), cplex.getDuals(visitorConstraints), cplex.getDual(numberOfVehiclesConstraint), cplex.getObjValue());
        }

        private Solution(IloCplex.Status status, double[] primalValues, double[] visitorDualValues, double numberOfVehiclesDualValue, double objectiveValue) {
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

    public class IntegerSolution {
        private final IloCplex.Status status;
        private final List<Double> primalValues;
        private final double objectiveValue;

        private IntegerSolution() throws IloException {
            this(cplex.getStatus(), cplex.getValues(theta), cplex.getObjValue());
        }

        private IntegerSolution(IloCplex.Status status, double[] primalValues, double objectiveValue) {
            this.status = status;
            this.primalValues = Arrays.stream(primalValues).boxed().collect(Collectors.toList());
            this.objectiveValue = objectiveValue;
        }

        public List<ElementaryPath> getUsedPaths() {
            List<ElementaryPath> ret = new ArrayList<>();
            for (int i = 0; i < paths.size(); i++) {
                if (Math.round(primalValues.get(i)) == 1){
                    ret.add(paths.get(i));
                }
            }
            return ret;
        }
    }
}
