package algorithm;

import algorithm.branching.Branch;
import algorithm.branching.BranchOnFleetSize;
import algorithm.branching.BranchOnVisitFlow;
import commons.Route;
import commons.Utils;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class RestrictedMasterProblem {

    private final List<Route>   allPaths;
    private final Deque<Branch> activeBranches;
    private RMPLinearSolution linearSolution;
    private RMPIntegerSolution  integerSolution;
    private List<Route>         activePaths;

    public RestrictedMasterProblem() {
        this.linearSolution = null;
        this.integerSolution = null;
        this.allPaths = new ArrayList<>();
        this.activePaths = new ArrayList<>();
        this.activeBranches = new ArrayDeque<>();

    }

    public void addColumns(List<Route> columns) {
        allPaths.addAll(columns);
    }

    public abstract void buildModel(IloCplex cplex, boolean integral, Duration remainingTime);

    public abstract RMPLinearSolution buildSolution(IloCplex cplex);

    public abstract RMPIntegerSolution buildIntegerSolution(IloCplex cplex);

    public abstract void performBranchOnVisitFlow(IloCplex cplex, BranchOnVisitFlow branch);

    public abstract void performBranchOnFleetSize(IloCplex cplex, BranchOnFleetSize branch);

    public void addBranch(Branch branch) {
        activeBranches.addLast(branch);
    }

    public void removeBranch(Branch branch) {
        activeBranches.removeLast();
    }

    public RMPLinearSolution getSolution() {
        if (linearSolution == null) {
            throw new IllegalStateException("Should have called solveRelaxation() before");
        }
        return linearSolution;
    }

    public RMPIntegerSolution getIntegerSolution() {
        if (integerSolution == null) {
            throw new IllegalStateException("Should have called solveInteger() before");
        }
        return integerSolution;
    }

    private boolean isCompatible(Route path) {
        return activeBranches.stream().allMatch(branch -> branch.isCompatible(path));
    }

    public void solveRelaxation() {
        solveRelaxation(Utils.DEFAULT_TIMEOUT);
    }

    public void solveRelaxation(Duration remainingTime) {
        try (IloCplex cplex = new IloCplex()) {
            this.activePaths = allPaths.stream().filter(this::isCompatible).toList();
            buildModel(cplex, false, remainingTime);
            performBranching(cplex);

            cplex.solve();
            linearSolution = buildSolution(cplex);
            cplex.end();
        } catch (IloException e) {
            linearSolution = new RMPLinearSolution();
        }
    }

    public void solveInteger() {
        solveInteger(Utils.DEFAULT_TIMEOUT);
    }

    public void solveInteger(Duration remainingTime) {
        try (IloCplex cplex = new IloCplex()) {
            this.activePaths = allPaths.stream().filter(this::isCompatible).toList();
            buildModel(cplex, true, remainingTime);

            performBranching(cplex);

            cplex.solve();
            integerSolution = buildIntegerSolution(cplex);
            cplex.end();
        } catch (IloException e) {
            integerSolution = new RMPIntegerSolution();
        }
    }

    private void performBranching(IloCplex cplex) {
        for (Branch branch : activeBranches) {
            if (branch instanceof BranchOnVisitFlow) {
                performBranchOnVisitFlow(cplex, (BranchOnVisitFlow) branch);
            } else if (branch instanceof BranchOnFleetSize) {
                performBranchOnFleetSize(cplex, (BranchOnFleetSize) branch);
            }
        }
    }

    public List<Route> getActivePaths() {
        return activePaths;
    }
}
