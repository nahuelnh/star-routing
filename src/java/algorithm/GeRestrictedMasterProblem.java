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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeRestrictedMasterProblem implements RestrictedMasterProblem {
    private static final double EPSILON = 0.01;
    private final Instance instance;
    private final List<FeasiblePath> allPaths;
    private final Deque<BranchingDirection> activeBranches;
    private List<FeasiblePath> activePaths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloRange[] customerConstraints;
    private IloRange vehiclesConstraint;
    private Map<BranchOnEdge, IloRange> fluxConstraints;

    public GeRestrictedMasterProblem(Instance instance) {
        this.instance = instance;
        this.allPaths = new ArrayList<>();
        this.activePaths = new ArrayList<>();
        this.activeBranches = new ArrayDeque<>();
    }

    @Override
    public void addPaths(List<FeasiblePath> newPaths) {
        allPaths.addAll(newPaths);
    }

    private boolean isCompatible(FeasiblePath path) {
        return activeBranches.stream().allMatch(branch -> branch.isCompatible(path));
    }

    private void createVariables(boolean integral) throws IloException {
        theta = new IloNumVar[activePaths.size()];
        for (int i = 0; i < activePaths.size(); i++) {
            theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
        }
    }

    private void createCustomerServedConstraints() throws IloException {
        customerConstraints = new IloRange[instance.getNumberOfCustomers()];
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = cplex.linearNumExpr();
            int customer = instance.getCustomer(s);
            for (int route = 0; route < activePaths.size(); route++) {
                if (activePaths.get(route).isCustomerServed(customer)) {
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
        for (int i = 0; i < activePaths.size(); i++) {
            objective.addTerm(theta[i], activePaths.get(i).getCost());
        }
        cplex.addMinimize(objective, "cost");
    }

    private void buildModel(boolean integral, Duration remainingTime) throws IloException {
        this.activePaths = allPaths.stream().filter(this::isCompatible).toList();
        System.out.println("Size: " + activePaths.size() + " from " + allPaths.size());
        cplex = new IloCplex();
        cplex.setOut(null);
        cplex.setParam(IloCplex.Param.TimeLimit, remainingTime.getSeconds() + 1);
        createVariables(integral);
        createCustomerServedConstraints();
        createNumberOfVehiclesConstraint();
        createObjective();
        fluxConstraints = new HashMap<>();
        for (BranchingDirection branch : activeBranches) {
            performBranching(branch);
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

            Map<BranchOnEdge, Double> fluxDuals = new HashMap<>();
            for (Map.Entry<BranchOnEdge, IloRange> entry : fluxConstraints.entrySet()) {
                fluxDuals.put(entry.getKey(), cplex.getDual(entry.getValue()));
            }

            RMPSolution solution = new RMPSolution(cplex.getObjValue(), cplex.getDuals(customerConstraints),
                    cplex.getDual(vehiclesConstraint), fluxDuals, cplex.getValues(theta), feasible, isIntegerSolution(),
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
            for (int i = 0; i < activePaths.size(); i++) {
                if (Utils.getBoolValue(cplex, theta[i])) {
                    ret.add(activePaths.get(i));
                }
            }
            postProcess(ret);
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addBranch(BranchingDirection branch) {
        this.activeBranches.addLast(branch);
    }

    private void performBranching(BranchingDirection branch) {
        if (branch instanceof BranchOnEdge) {
            performBranchOnEdge((BranchOnEdge) branch);
        }
    }

    private void performBranchOnEdge(BranchOnEdge branch) {
        try {
            IloNumExpr flux = cplex.linearNumExpr();
            int terms = 0;
            for (int route = 0; route < activePaths.size(); route++) {
                if (activePaths.get(route).containsEdge(branch.getStart(), branch.getEnd())) {
                    flux = cplex.sum(flux, theta[route]);
                    terms++;
                }
            }
            if (terms > 0) {
                if (branch.isLowerBound()) {
                    fluxConstraints.put(branch, cplex.addGe(flux, branch.getBound()));
                } else {
                    fluxConstraints.put(branch, cplex.addLe(flux, branch.getBound()));
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeBranch(BranchingDirection branch) {
        activeBranches.removeLast();
    }

    @Override
    public double fluxOnEdge(int start, int end) {
        try {
            IloNumExpr flux = cplex.linearNumExpr();
            for (int route = 0; route < activePaths.size(); route++) {
                if (activePaths.get(route).containsEdge(start, end)) {
                    flux = cplex.sum(flux, theta[route]);
                }
            }
            return cplex.getValue(flux);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isIntegerSolution() throws IloException {
        for (int route = 0; route < activePaths.size(); route++) {
            double value = cplex.getValue(theta[route]);
            double fractionalPart = Math.abs(value - (int) (value + 0.5));
            if (fractionalPart > EPSILON) {
                return false;
            }
        }
        return true;
    }

}
