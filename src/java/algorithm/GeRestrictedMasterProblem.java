package algorithm;

import algorithm.branching.BranchOnEdge;
import algorithm.branching.BranchOnVisitFlow;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeRestrictedMasterProblem extends RestrictedMasterProblem {

    private static final double EPSILON = 0.01;
    private final Instance instance;
    private IloNumVar[] theta;
    private IloRange[] customerConstraints;
    private IloRange vehiclesConstraint;
    private Map<BranchOnEdge, IloRange> branchOnEdgeConstraints;
    private Map<BranchOnVisitFlow, IloRange> branchOnVisitFlowConstraints;

    public GeRestrictedMasterProblem(Instance instance) {
        this.instance = instance;
    }

    private void createVariables(IloCplex cplex, boolean integral) throws IloException {
        theta = new IloNumVar[getActivePaths().size()];
        for (int i = 0; i < getActivePaths().size(); i++) {
            theta[i] = integral ? cplex.boolVar("theta_" + i) : cplex.numVar(0, 1, "theta_" + i);
        }
    }

    private void createCustomerServedConstraints(IloCplex cplex) throws IloException {
        customerConstraints = new IloRange[instance.getNumberOfCustomers()];
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = cplex.linearNumExpr();
            int customer = instance.getCustomer(s);
            for (int route = 0; route < getActivePaths().size(); route++) {
                if (getActivePaths().get(route).isCustomerServed(customer)) {
                    lhs = cplex.sum(lhs, theta[route]);
                }
            }
            customerConstraints[s] = cplex.addGe(lhs, 1, "customer_served_" + s);
        }
    }

    private void createNumberOfVehiclesConstraint(IloCplex cplex) throws IloException {
        IloNumExpr numberOfRoutesUsed = Utils.getArraySum(cplex, theta);
        if (instance.unusedVehiclesAllowed()) {
            vehiclesConstraint = cplex.addLe(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
        } else {
            vehiclesConstraint = cplex.addEq(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
        }
    }

    private void createObjective(IloCplex cplex) throws IloException {
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < getActivePaths().size(); i++) {
            objective.addTerm(theta[i], getActivePaths().get(i).getCost());
        }
        cplex.addMinimize(objective, "cost");
    }

    @Override
    public void buildModel(IloCplex cplex, boolean integral, Duration remainingTime) {
        try {
            cplex.setOut(null);
            cplex.setParam(IloCplex.Param.TimeLimit, remainingTime.getSeconds() + 1);
            createVariables(cplex, integral);
            createCustomerServedConstraints(cplex);
            createNumberOfVehiclesConstraint(cplex);
            createObjective(cplex);
            branchOnEdgeConstraints = new HashMap<>();
            branchOnVisitFlowConstraints = new HashMap<>();
        } catch (IloException e) {
            throw new RuntimeException(e);
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

    public List<FeasiblePath> computePathsFromSolution(IloCplex cplex) {
        try {
            List<FeasiblePath> ret = new ArrayList<>();
            if (!Utils.isSolutionFeasible(cplex)) {
                return ret;
            }
            for (int i = 0; i < getActivePaths().size(); i++) {
                if (Utils.getBoolValue(cplex, theta[i])) {
                    ret.add(getActivePaths().get(i));
                }
            }
            postProcess(ret);
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void performBranchOnEdge(IloCplex cplex, BranchOnEdge branch) {
        try {
            IloNumExpr flow = cplex.linearNumExpr();
            int numberOfTerms = 0;
            for (int route = 0; route < getActivePaths().size(); route++) {
                if (getActivePaths().get(route).containsEdge(branch.getStart(), branch.getEnd())) {
                    flow = cplex.sum(flow, theta[route]);
                    numberOfTerms++;
                }
            }
            if (numberOfTerms > 0) {
                if (branch.isLowerBound()) {
                    branchOnEdgeConstraints.put(branch, cplex.addGe(flow, branch.getBound()));
                } else {
                    branchOnEdgeConstraints.put(branch, cplex.addLe(flow, branch.getBound()));
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void performBranchOnVisitFlow(IloCplex cplex, BranchOnVisitFlow branch) {
        try {
            IloNumExpr flow = cplex.linearNumExpr();
            int numberOfTerms = 0;
            for (int route = 0; route < getActivePaths().size(); route++) {
                FeasiblePath currentPath = getActivePaths().get(route);
                if (currentPath.containsEdge(branch.getStart(), branch.getEnd()) &&
                        currentPath.isCustomerServed(branch.getCustomer())) {
                    flow = cplex.sum(flow, theta[route]);
                    numberOfTerms++;
                }
            }
            if (numberOfTerms > 0) {
                if (branch.isLowerBound()) {
                    branchOnVisitFlowConstraints.put(branch, cplex.addGe(flow, branch.getBound()));
                } else {
                    branchOnVisitFlowConstraints.put(branch, cplex.addLe(flow, branch.getBound()));
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, Map<Integer, Double>> getFlowOnEdges(IloCplex cplex) {
        Map<Integer, Map<Integer, Double>> flow = new HashMap<>();
        try {
            for (int start = 0; start < instance.getNumberOfNodes(); start++) {
                for (int end = 0; end < instance.getNumberOfNodes(); end++) {
                    if (start != end) {
                        flow.putIfAbsent(start, new HashMap<>());
                        IloNumExpr flowOnEdge = cplex.linearNumExpr();
                        for (int route = 0; route < getActivePaths().size(); route++) {
                            if (getActivePaths().get(route).containsEdge(start, end)) {
                                flowOnEdge = cplex.sum(flowOnEdge, theta[route]);
                            }
                        }
                        flow.get(start).put(end, cplex.getValue(flowOnEdge));
                    }
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return flow;
    }

    private Map<Integer, Map<Integer, Map<Integer, Double>>> getVisitFlow(IloCplex cplex) {
        Map<Integer, Map<Integer, Map<Integer, Double>>> flow = new HashMap<>();
        try {
            for (int start = 0; start < instance.getNumberOfNodes(); start++) {
                for (int end = 0; end < instance.getNumberOfNodes(); end++) {
                    for (int customer : instance.getCustomers()) {
                        if (start != end) {
                            IloNumExpr flowOnEdge = cplex.linearNumExpr();
                            for (int route = 0; route < getActivePaths().size(); route++) {
                                FeasiblePath path = getActivePaths().get(route);
                                if (path.containsEdge(start, end) && path.isCustomerServed(customer)) {
                                    flowOnEdge = cplex.sum(flowOnEdge, theta[route]);
                                }
                            }
                            double value = cplex.getValue(flowOnEdge);
                            if (value > EPSILON && value < 1 - EPSILON) {
                                flow.putIfAbsent(start, new HashMap<>());
                                flow.get(start).putIfAbsent(end, new HashMap<>());
                                flow.get(start).get(end).put(customer, value);
                            }
                        }
                    }
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return flow;
    }

    @Override
    public RMPLinearSolution buildSolution(IloCplex cplex) {
        try {
            if (!Utils.isSolutionFeasible(cplex)) {
                return new RMPLinearSolution();
            }
            return new RMPLinearSolution(cplex.getObjValue(), cplex.getDuals(customerConstraints),
                    cplex.getDual(vehiclesConstraint), true, cplex.getValues(theta), isIntegerSolution(cplex),
                    getFlowOnEdges(cplex), getFlowDuals(cplex), getVisitFlow(cplex), new HashMap<>());
        } catch (IloException e) {
            return new RMPLinearSolution();
        }
    }

    private Map<BranchOnEdge, Double> getFlowDuals(IloCplex cplex) throws IloException {
        Map<BranchOnEdge, Double> flowDuals = new HashMap<>();
        for (BranchOnEdge branch : branchOnEdgeConstraints.keySet()) {
            flowDuals.put(branch, cplex.getDual(branchOnEdgeConstraints.get(branch)));
        }
        return flowDuals;
    }

    private Map<BranchOnVisitFlow, Double> getVisitFlowDuals(IloCplex cplex) throws IloException {
        Map<BranchOnVisitFlow, Double> flowDuals = new HashMap<>();
        for (BranchOnVisitFlow branch : branchOnVisitFlowConstraints.keySet()) {
            flowDuals.put(branch, cplex.getDual(branchOnVisitFlowConstraints.get(branch)));
        }
        return flowDuals;
    }

    @Override
    public RMPIntegerSolution buildIntegerSolution(IloCplex cplex) {
        try {
            if (!Utils.isSolutionFeasible(cplex)) {
                return new RMPIntegerSolution();
            }
            return new RMPIntegerSolution(cplex.getObjValue(), computePathsFromSolution(cplex), true);
        } catch (IloException e) {
            return new RMPIntegerSolution();
        }
    }

    private boolean isIntegerSolution(IloCplex cplex) throws IloException {
        for (int route = 0; route < getActivePaths().size(); route++) {
            double value = cplex.getValue(theta[route]);
            double fractionalPart = Math.abs(value - (int) (value + 0.5));
            if (fractionalPart > EPSILON) {
                return false;
            }
        }
        return true;
    }
}
