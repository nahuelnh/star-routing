package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqRestrictedMasterProblem implements RestrictedMasterProblem {
    private static final double EPSILON = 0.01;
    private final Instance instance;
    private final List<FeasiblePath> paths;
    private final Map<BranchingDirection, IloRange> branchingInequalities;
    private BranchingDirection branch;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerConstraints;
    private IloRange vehiclesConstraint;

    public EqRestrictedMasterProblem(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
        this.branchingInequalities = new HashMap<>();
        this.branch = null;
    }

    @Override
    public void addPaths(List<FeasiblePath> newPaths) {
        paths.addAll(newPaths);
    }

    private void createVariables(boolean integral) throws IloException {
        theta = new IloNumVar[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            if (branch != null && branch.isCompatible(paths.get(i))) {
                theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
            }
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

    private void buildModel(boolean integral, Duration remainingTime) throws IloException {
        cplex = new IloCplex();
        cplex.setOut(null);
        cplex.setParam(IloCplex.Param.TimeLimit, remainingTime.getSeconds() + 1);
        createVariables(integral);
        createCustomerServedConstraints();
        createNumberOfVehiclesConstraint();
        createObjective();
        for (IloConstraint constraint : branchingInequalities.values()) {
            cplex.add(constraint);
        }
    }

    @Override
    public RMPSolution solveRelaxation(Duration remainingTime) {
        try {
            buildModel(false, remainingTime);
            cplex.solve();
            boolean feasible = IloCplex.Status.Optimal.equals(cplex.getStatus());
            Map<Integer, Map<Integer, Double>> flux = new HashMap<>();
            for (int start = 0; start < instance.getNumberOfNodes(); start++) {
                for (int end = 0; end < instance.getNumberOfNodes(); end++) {
                    if (start != end) {
                        flux.putIfAbsent(start, new HashMap<>());
                        flux.get(start).put(end, fluxOnEdge(start, end));
                    }
                }
            }

            RMPSolution solution = new RMPSolution(cplex.getObjValue(), cplex.getDuals(customerConstraints),
                    cplex.getDual(vehiclesConstraint), new HashMap<>(), cplex.getValues(theta), feasible, isIntegerSolution(),
                    flux);
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

    @Override
    public void addBranch(BranchingDirection branch) {
        this.branch = branch;
        if (branch instanceof BranchOnEdge) {
            performBranchOnEdge((BranchOnEdge) branch);
        }
    }

    private void performBranchOnEdge(BranchOnEdge branch) {
        try {
            IloNumExpr flux = cplex.linearNumExpr();
            for (int route = 0; route < paths.size(); route++) {
                if (paths.get(route).containsEdge(branch.getStart(), branch.getEnd())) {
                    flux = cplex.sum(flux, theta[route]);
                }
            }
            if (branch.isLowerBound()) {
                branchingInequalities.put(branch, cplex.addGe(flux, branch.getBound()));
            } else {
                branchingInequalities.put(branch, cplex.addLe(flux, branch.getBound()));
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeBranch(BranchingDirection branch) {
        if (branch instanceof BranchOnEdge) {
            reverseBranchOnEdge((BranchOnEdge) branch);
        }
    }

    private void reverseBranchOnEdge(BranchOnEdge branch) {
        branchingInequalities.remove(branch);
    }

    @Override
    public double fluxOnEdge(int start, int end) {
        try {
            IloNumExpr flux = cplex.linearNumExpr();
            for (int route = 0; route < paths.size(); route++) {
                if (paths.get(route).containsEdge(start, end)) {
                    flux = cplex.sum(flux, theta[route]);
                }
            }
            return cplex.getValue(flux);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isIntegerSolution() throws IloException {
        for (int route = 0; route < paths.size(); route++) {
            double value = cplex.getValue(theta[route]);
            double fractionalPart = Math.abs(value - (int) (value + 0.5));
            if (fractionalPart > EPSILON) {
                return false;
            }
        }
        return true;
    }

}
