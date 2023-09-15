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

public class FirstStepPricingProblem implements PricingProblem {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final List<FeasiblePath> paths;
    private IloCplex cplex;
    private IloNumVar[][] theta;
    private IloNumVar[][][] omega;
    private IloNumVar[][][] y;

    public FirstStepPricingProblem(Instance instance) {
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

        theta = new IloNumVar[N][K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                theta[i][k] = cplex.numVar(0, 1, "theta_" + i + "_" + k);
            }
        }
        omega = new IloNumVar[S][N][K];
        for (int s = 0; s < S; s++) {
            for (int r = 0; r < N; r++) {
                for (int k = 0; k < K; k++) {
                    omega[s][r][k] = cplex.numVar(0, 1, "omega_" + s + "_" + r + "_" + k);
                }
            }
        }

        y = new IloNumVar[S][N][K];
        for (int s = 0; s < S; s++) {
            for (int r = 0; r < N; r++) {
                for (int k = 0; k < K; k++) {
                    y[s][r][k] = cplex.boolVar( "y_" + s + "_" + r + "_" + k);
                }
            }
        }
    }

    private void createLinkingConstraints() throws IloException {
        for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
            for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                for (int r = 0; r < paths.size(); r++) {
                    // IloNumExpr rhs = cplex.linearNumExpr();
                    int rhs = 0;
                    for (int neighbor : instance.getNeighbors(instance.getCustomer(s))) {
                        if (paths.get(r).containsNode(neighbor)) {
                            //rhs = cplex.sum(rhs, theta[r][k]);
                            rhs = 1;
                        }
                    }
                    cplex.addLe(y[s][r][k], rhs, "linking_" + s + "_" + r + "_" + k);
                }
            }
        }
    }

    private void createCapacityConstraints() throws IloException {
        for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
            for (int r = 0; r < paths.size(); r++) {
                IloLinearNumExpr totalDemand = cplex.linearNumExpr();
                for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
                    totalDemand.addTerm(y[s][r][k], instance.getDemand(instance.getCustomer(s)));
                }
                //   cplex.addLe(totalDemand,  cplex.prod(instance.getCapacity(), theta[r][k]), "capacity_" + r);
                cplex.addLe(totalDemand, instance.getCapacity(), "capacity_" + r);
            }
        }
    }

    private void createCustomerServedConstraints() throws IloException {
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            IloNumExpr lhs = cplex.linearNumExpr();
            for (int r = 0; r < paths.size(); r++) {
                for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                    lhs = cplex.sum(lhs, omega[s][r][k]);
                }
            }
            cplex.addEq(lhs, 1, "customer_served_" + s);
        }
    }

    private void createNumberOfVehiclesConstraint() throws IloException {
        IloNumExpr numberOfRoutesUsed = cplex.linearNumExpr();
        for (int r = 0; r < paths.size(); r++) {
            for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                numberOfRoutesUsed = cplex.sum(numberOfRoutesUsed, theta[r][k]);
            }
        }
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

        int M = 2;
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            for (int r = 0; r < paths.size(); r++) {
                for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                    //                    IloIntVar omega = cplex.boolVar();
                    //                    cplex.addLe(cplex.diff(y[s][r][k], theta[r][k]), cplex.prod(M, cplex.diff(1, omega)));
                    //                    cplex.addLe(y[s][r][k], cplex.prod(M, omega));
                    cplex.addLe(omega[s][r][k], y[s][r][k]);
                    cplex.addLe(omega[s][r][k], theta[r][k]);
                    cplex.addGe(omega[s][r][k], cplex.diff(cplex.sum(theta[r][k], y[s][r][k]), 1));

                }
            }
        }
    }

    private void createObjective() throws IloException {
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < paths.size(); i++) {
            for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                objective.addTerm(theta[i][k], paths.get(i).getCost());
            }
        }
        cplex.addMinimize(objective, "cost");
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        try {
            cplex = new IloCplex();
            //cplex.setOut(null);
            createVariables();
            createConstraints();
            createObjective();
            cplex.solve();
            System.out.println("--------");
            Utils.printNonZero(cplex, theta);
            computePathsFromSolution().forEach(System.out::println);

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

    private FeasiblePath getPathFromR(int route, int k) throws IloException {
        FeasiblePath feasiblePath = FeasiblePath.getCopyWithoutCustomers(paths.get(route));
        Set<Integer> customers = new HashSet<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            if (cplex.getValue(y[s][route][k]) - EPSILON > 0) {
                customers.add(instance.getCustomer(s));
            }
        }
        feasiblePath.addCustomers(customers);
        return feasiblePath;
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        try {
            List<FeasiblePath> ret = new ArrayList<>();
            if (!Utils.isSolutionFeasible(cplex)) {
                return ret;
            }
            for (int r = 0; r < paths.size(); r++) {
                for (int k = 0; k < instance.getNumberOfVehicles(); k++) {
                    if (Math.ceil(cplex.getValue(theta[r][k]) - EPSILON) > 0) {
                        ret.add(getPathFromR(r, k));
                    }
                }
            }
            return ret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
}
