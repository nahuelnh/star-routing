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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeRestrictedMasterProblem implements RestrictedMasterProblem {

    private final Instance instance;
    private final List<FeasiblePath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerConstraints;
    private IloRange vehiclesConstraint;

    public GeRestrictedMasterProblem(Instance instance) {
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
            customerConstraints[s] = cplex.addGe(lhs, 1, "customer_served_" + s);
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

    private void buildModel(boolean integral, Duration remainingTime) throws IloException {
        cplex = new IloCplex();
        cplex.setOut(null);
        cplex.setParam(IloCplex.Param.TimeLimit, remainingTime.getSeconds() + 1);
        createVariables(integral);
        createCustomerServedConstraints();
        createNumberOfVehiclesConstraint();
        createObjective();
    }

    @Override
    public RMPSolution solveRelaxation(Duration remainingTime) {
        try {
            buildModel(false, remainingTime);
            cplex.solve();
            boolean feasible = IloCplex.Status.Optimal.equals(cplex.getStatus());
            RMPSolution solution = new RMPSolution(cplex.getObjValue(), cplex.getDuals(customerConstraints),
                    cplex.getDual(vehiclesConstraint), cplex.getValues(theta), feasible);
            cplex.end();
            return solution;
        } catch (IloException e) {
            cplex.end();
            return new RMPSolution();
        }
    }

    @Override
    public RMPIntegerSolution solveInteger(Duration remainingTime) {
        try {
            buildModel(true, remainingTime);
            cplex.solve();
            boolean feasible = IloCplex.Status.Optimal.equals(cplex.getStatus());
            List<FeasiblePath> pathsFromSolution = feasible ? computePathsFromSolution() : new ArrayList<>();
            RMPIntegerSolution solution = new RMPIntegerSolution(cplex.getObjValue(), pathsFromSolution, feasible);
            cplex.end();
            return solution;
        } catch (IloException e) {
            cplex.end();
            return new RMPIntegerSolution();
        }
    }

    private void postProcess(List<FeasiblePath> paths) {
        Set<Integer> customersProcessed = new HashSet<>();
        for (FeasiblePath path : paths) {
            for (Integer customer : customersProcessed) {
                if (path.isCustomerServed(customer)) {
                    path.removeCustomer(customer);
                }
            }
            customersProcessed.addAll(path.getCustomersServed());
        }
        paths.removeIf(path -> path.getCustomersServed().isEmpty());
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
            postProcess(ret);
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

}
