package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnEdge;
import algorithm.branching.BranchingDirection;
import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelSettingPricing implements PricingProblem {

    private final Instance instance;
    private final boolean solveHeuristically;
    private final Deque<BranchingDirection> activeBranches;
    private boolean forceExactSolution;
    private List<FeasiblePath> paths;
    private ESPPRCGraph graph;

    public LabelSettingPricing(Instance instance) {
        this(instance, false);
    }

    public LabelSettingPricing(Instance instance, boolean solveHeuristically) {
        this.instance = instance;
        this.paths = new ArrayList<>();
        this.solveHeuristically = solveHeuristically;
        this.forceExactSolution = false;
        this.activeBranches = new ArrayDeque<>();
        this.graph = new ESPPRCGraph(instance);
    }

    private double getObjValue(FeasiblePath path, Map<Integer, Double> dualValues, double vehiclesDual) {
        double sum = path.getCustomersServed().stream().map(dualValues::get).reduce(Double::sum).orElse(0.0);
        return path.getCost() - sum - vehiclesDual;
    }

    private double getMinObjValue(RMPLinearSolution rmpSolution) {
        Map<Integer, Double> dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        return paths.stream().mapToDouble(path -> getObjValue(path, dualValues, rmpSolution.getVehiclesDual())).min()
                .orElse(0.0);
    }

    @Override
    public PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime) {
        Instant start = Instant.now();
        graph = new ESPPRCGraph(instance);

        for (BranchingDirection branch : activeBranches) {
            performBranching(branch);
        }

        LabelSettingAlgorithm labelSettingAlgorithm =
                new LabelSettingAlgorithm(instance, rmpSolution, activeBranches, graph, !forceExactSolution);
        paths = labelSettingAlgorithm.run(remainingTime);
        int labelsProcessed = labelSettingAlgorithm.getLabelsProcessed();
        if (paths.isEmpty() && !solveHeuristically) {
            labelSettingAlgorithm = new LabelSettingAlgorithm(instance, rmpSolution, activeBranches, graph, false);
            paths = labelSettingAlgorithm.run(Utils.getRemainingTime(start, remainingTime));
            labelsProcessed += labelSettingAlgorithm.getLabelsProcessed();
        }
        forceExactSolution = false;
        return new PricingSolution(getMinObjValue(rmpSolution), paths, labelsProcessed, true);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }

    @Override
    public void forceExactSolution() {
        this.forceExactSolution = true;
    }

    @Override
    public void addBranch(BranchingDirection branch) {
        activeBranches.add(branch);
    }

    private void performBranching(BranchingDirection branch) {
        if (branch instanceof BranchOnEdge) {
            performBranchOnEdge((BranchOnEdge) branch);
        }
    }

    private void performBranchOnEdge(BranchOnEdge branch) {
        if (!branch.isLowerBound() && branch.getBound() == 0) {
            graph.removeEdge(branch.getStart(), branch.getEnd());
        }
        if (branch.isLowerBound() && branch.getBound() >= 1) {
            for (int node : graph.getAdjacentNodes(branch.getStart())) {
                if (branch.getEnd() == instance.getDepot() && node != graph.getEnd()) {
                    graph.removeEdge(graph.getStart(), node);
                }
                if (branch.getEnd() != instance.getDepot() && node != branch.getEnd()) {
                    graph.removeEdge(graph.getStart(), node);
                }
            }
        }
    }

    @Override
    public void removeBranch(BranchingDirection branch) {
        activeBranches.removeLast();
    }
}
