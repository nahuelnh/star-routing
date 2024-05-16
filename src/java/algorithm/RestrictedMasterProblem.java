package algorithm;

import algorithm.branching.Branch;
import algorithm.branching.BranchOnEdge;
import algorithm.branching.BranchOnVisitFlow;
import commons.FeasiblePath;
import commons.Utils;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class RestrictedMasterProblem {

    private final List<FeasiblePath> allPaths;
    private final Deque<Branch> activeBranches;
    private RMPLinearSolution linearSolution;
    private RMPIntegerSolution integerSolution;
    private List<FeasiblePath> activePaths;

    public RestrictedMasterProblem() {
        this.linearSolution = null;
        this.integerSolution = null;
        this.allPaths = new ArrayList<>();
        this.activePaths = new ArrayList<>();
        this.activeBranches = new ArrayDeque<>();

    }

    public void addColumns(List<FeasiblePath> columns) {
        allPaths.addAll(columns);
    }

    public abstract void buildModel(IloCplex cplex, boolean integral, Duration remainingTime);

    public abstract RMPLinearSolution buildSolution(IloCplex cplex);

    public abstract RMPIntegerSolution buildIntegerSolution(IloCplex cplex);

    abstract void performBranchOnEdge(IloCplex cplex, BranchOnEdge branch);

    abstract void performBranchOnVisitFlow(IloCplex cplex, BranchOnVisitFlow branch);

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

    private boolean isCompatible(FeasiblePath path) {
        return activeBranches.stream().allMatch(branch -> branch.isCompatible(path));
    }

    public void solveRelaxation() {
        solveRelaxation(Utils.DEFAULT_TIMEOUT);
    }

    public void solveRelaxation(Duration remainingTime) {
        try (IloCplex cplex = new IloCplex()) {
            this.activePaths = allPaths.stream().filter(this::isCompatible).toList();
            buildModel(cplex, false, remainingTime);
            for (Branch branch : activeBranches) {
                performBranching(cplex, branch);
            }
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
            for (Branch branch : activeBranches) {
                performBranching(cplex, branch);
            }
            cplex.solve();
            integerSolution = buildIntegerSolution(cplex);
            cplex.end();
        } catch (IloException e) {
            integerSolution = new RMPIntegerSolution();
        }
    }

    private void performBranching(IloCplex cplex, Branch branch) {
        if (branch instanceof BranchOnEdge) {
            performBranchOnEdge(cplex, (BranchOnEdge) branch);
        }
    }

    public List<FeasiblePath> getAllPaths() {
        return allPaths;
    }

    public Deque<Branch> getActiveBranches() {
        return activeBranches;
    }

    public List<FeasiblePath> getActivePaths() {
        return activePaths;
    }
}
