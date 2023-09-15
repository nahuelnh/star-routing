package algorithm;

import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecondStepPricingProblem implements PricingProblem {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final List<FeasiblePath> paths;
    private IloCplex cplex;
    private IloNumVar[] theta;
    private IloNumVar[][] y;

    public SecondStepPricingProblem(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    public void addPaths(List<FeasiblePath> newPaths) {
        paths.addAll(newPaths);
    }

    private void createVariables() throws IloException {
        int N = paths.size();
        int S = instance.getNumberOfCustomers();
        int K = instance.getNumberOfVehicles();

        theta = new IloNumVar[N];
        for (int i = 0; i < N; i++) {
            theta[i] = cplex.numVar(0, K, "theta_" + i);
        }

        y = new IloNumVar[S][N];
        for (int s = 0; s < S; s++) {
            for (int r = 0; r < N; r++) {
                y[s][r] = cplex.numVar(0, 1, "y_" + s + "_" + r);
            }
        }
    }

    private void createLinkingConstraints() throws IloException {
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            for (int r = 0; r < paths.size(); r++) {
                IloNumExpr rhs = cplex.linearNumExpr();
                for (int neighbor : instance.getNeighbors(instance.getCustomer(s))) {
                    if (paths.get(r).containsNode(neighbor)) {
                        rhs = cplex.sum(rhs, theta[r]);
                    }
                }
                cplex.addLe(y[s][r], rhs, "linking_" + s + "_" + r);
            }
        }
    }

    private void createCapacityConstraints() throws IloException {
        for (int r = 0; r < paths.size(); r++) {
            IloLinearNumExpr totalDemand = cplex.linearNumExpr();
            for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                totalDemand.addTerm(y[s][r], instance.getDemand(instance.getCustomer(s)));
            }
            cplex.addLe(totalDemand, instance.getCapacity() * instance.getNumberOfVehicles(), "capacity_" + r);
        }
    }

    private void createCustomerServedConstraints() throws IloException {
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = Utils.getRowSum(cplex, y, s);
            cplex.addEq(lhs, 1, "customer_served_" + s);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloNumExpr numberOfRoutesUsed = Utils.getArraySum(cplex, theta);
        if (instance.unusedVehiclesAllowed()) {
            cplex.addLe(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
        } else {
            cplex.addEq(numberOfRoutesUsed, instance.getNumberOfVehicles(), "number_vehicles");
        }
    }

    private void createConstraints() throws IloException {
        createLinkingConstraints();
        createCapacityConstraints();
        createCustomerServedConstraints();
        createNumberOfVehiclesConstraint();
    }

    private void createObjective() throws IloException {
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            objective.addTerm(theta[i], paths.get(i).getCost());
        }
        cplex.addMinimize(objective, "cost");
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            createVariables();
            createConstraints();
            createObjective();
            cplex.solve();
            System.out.println("--------");
            Utils.printNonZero(cplex, theta);
            computePathsFromSolution().forEach(System.out::println);

            //                        Utils.printNonZero(cplex, y);
            System.out.println(cplex.getObjValue());
            System.out.println("********");
            System.out.println();
            Solution solution = new Solution(cplex.getStatus(), cplex.getObjValue(), this);
            cplex.end();
            return solution;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private List<FeasiblePath> getPathFromR(int route) throws IloException {
        int size = (int) Math.round(Math.ceil(cplex.getValue(theta[route]) - EPSILON));
        List<FeasiblePath> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(FeasiblePath.getCopyWithoutCustomers(paths.get(route)));
        }

        Set<Integer> customers = new HashSet<>();
        int cumulativeDemand = 0;
        int currentIndex = 0;
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            if (cplex.getValue(y[s][route]) - EPSILON > 0) {
                cumulativeDemand += instance.getDemand(instance.getCustomer(s));
                if (cumulativeDemand > instance.getCapacity()) {
                    ret.get(currentIndex).addCustomers(customers);
                    customers.clear();
                    currentIndex++;
                    cumulativeDemand = instance.getDemand(instance.getCustomer(s));
                }
                customers.add(instance.getCustomer(s));
            }
        }
        ret.get(currentIndex).addCustomers(customers);
        return ret;
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        try {
            List<FeasiblePath> ret = new ArrayList<>();
            if (!Utils.isSolutionFeasible(cplex)) {
                return ret;
            }
            for (int r = 0; r < paths.size(); r++) {
                if (Math.ceil(cplex.getValue(theta[r]) - EPSILON) > 0) {
                    ret.addAll(getPathFromR(r));
                }
            }
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
}
