package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

public class EqRestrictedMasterProblem implements RestrictedMasterProblem {

    private final Instance instance;
    private final List<FeasiblePath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerConstraints;
    private IloRange vehiclesConstraint;

    public EqRestrictedMasterProblem(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    @Override
    public void addPaths(List<FeasiblePath> newPaths) {
        paths.addAll(newPaths);
    }

    private void createVariables(boolean integral) throws IloException {
        theta = new IloNumVar[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
        }
    }

    private void createCustomerServedConstraints() throws IloException {
        customerConstraints = new IloRange[instance.getNumberOfCustomers()];
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = cplex.linearNumExpr();
            int customer = instance.getCustomer(s);
            for (int route = 0; route < paths.size(); route++) {
                if (paths.get(route).isCustomerServed(customer)) {
                    lhs = cplex.sum(lhs, theta[route]);
                }
            }
            customerConstraints[s] = cplex.addEq(lhs, 1, "customer_served_" + s);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloNumExpr numberOfRoutesUsed = Utils.getArraySum(cplex, theta);
        if (instance.unusedVehiclesAllowed()) {
            vehiclesConstraint = cplex.addLe(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
        } else {
            vehiclesConstraint = cplex.addEq(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
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

    @Override
    public RMPSolution solveRelaxation() {
        try {
            buildModel(false);
            cplex.solve();
            RMPSolution solution = new RMPSolution(cplex, customerConstraints, vehiclesConstraint);
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RMPIntegerSolution solveInteger() {
        try {
            buildModel(true);
            cplex.solve();
            RMPIntegerSolution solution = new RMPIntegerSolution(cplex, this);
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
            for (int i = 0; i < paths.size(); i++) {
                if (Utils.getBoolValue(cplex, theta[i])) {
                    ret.add(paths.get(i));
                }
            }
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
}
