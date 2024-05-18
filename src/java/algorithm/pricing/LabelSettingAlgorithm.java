package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnVisitFlow;
import commons.FeasiblePath;
import commons.Instance;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LabelSettingAlgorithm {

    private static final double EPSILON = 1e-6d;
    private static final int STOP_AFTER_N_SOLUTIONS = 5;
    private final Instance instance;
    private final RMPLinearSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final Map<Integer, Double> dualValues;
    private final Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer;
    private final boolean applyHeuristics;
    private final boolean stopEarly = false;
    private final double alpha;
    private final LabelDump labelDump;
    private int labelsProcessed;
    private int solutionsFound;

    public LabelSettingAlgorithm(Instance instance, RMPLinearSolution rmpSolution, boolean applyHeuristics) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.dualValues = new HashMap<>();
        this.labelsProcessed = 0;
        this.solutionsFound = 0;
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        this.graph = new ESPPRCGraph(instance);
        this.applyHeuristics = applyHeuristics;
        this.alpha = computeCostFactor(graph);
        this.labelDump = selectLabelDump(applyHeuristics, graph, instance);
        this.branchesIndexedByCustomer = getBranchesIndexedByCustomer(rmpSolution);
    }

    public LabelSettingAlgorithm(Instance instance, RMPLinearSolution rmpSolution) {
        this(instance, rmpSolution, false);
    }

    private static Map<Integer, List<BranchOnVisitFlow>> getBranchesIndexedByCustomer(RMPLinearSolution rmpSolution) {
        Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer = new HashMap<>();
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            branchesIndexedByCustomer.putIfAbsent(branch.getCustomer(), new ArrayList<>());
            branchesIndexedByCustomer.get(branch.getCustomer()).add(branch);
        }
        return branchesIndexedByCustomer;
    }

    private static LabelDump selectLabelDump(boolean applyHeuristics, ESPPRCGraph graph, Instance instance) {
        if (applyHeuristics) {
            return new RelaxedLabelDump(graph.getSize(), instance.getCapacity() + 1);
        } else {
            return new StrictLabelDump(graph.getSize());
        }
    }

    private static double computeCostFactor(ESPPRCGraph graph) {
        int sum = 0;
        for (int i = 0; i < graph.getSize(); i++) {
            for (int j = 0; j < graph.getSize(); j++) {
                if (graph.containsEdge(i, j)) {
                    sum += graph.getEdge(i, j).getWeight();
                }
            }
        }
        return 1.0 / sum;
    }

    private List<FeasiblePath> getNegativeReducedCostPaths() {
        List<FeasiblePath> ret = new ArrayList<>();
        for (Label currentLabel : labelDump.getNegativeReducedCostLabels(graph.getEnd())) {
            ret.add(currentLabel.translateToFeasiblePath(graph));
        }
        return ret;
    }

    public List<FeasiblePath> run(Duration timeLimit) {
        Stopwatch stopwatch = new Stopwatch(timeLimit);
        monoDirectionalBacktracking(stopwatch);
        return getNegativeReducedCostPaths();
    }

    private double getLittleFakeCost(Label label, int customer) {
        if (graph.containsEdge(customer, label.node())) {
            return graph.getEdge(customer, label.node()).getWeight() * alpha;
        }
        return 0.0;
    }

    private Label extendCustomer(Label label, int customer) {
        int updatedDemand = label.demand() + instance.getDemand(customer);
        double updatedCost = label.cost() - dualValues.get(customer);
        if (applyHeuristics) {
            updatedCost += getLittleFakeCost(label, customer);
        }
        BitSet updatedVisited = label.copyOfVisitedCustomers();
        updatedVisited.set(customer);
        return new Label(updatedDemand, updatedCost, label.node(), label.copyOfVisitedNodes(), updatedVisited, label);

    }

    private Label extendNode(Label label, int nextNode) {
        double updatedCost = label.cost() + graph.getEdge(label.node(), nextNode).getWeight();
        BitSet updatedVisited = label.copyOfVisitedNodes();
        updatedVisited.set(nextNode);
        return new Label(label.demand(), updatedCost, nextNode, updatedVisited, label.copyOfVisitedCustomers(), label);
    }

    private boolean isCustomerUnreachable(Label label, int customer, Label previousLabel, LabelDump labelDump) {
        if (previousLabel.isCustomerVisited(customer)) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
            return true;
        }
        if (dualValues.get(customer) < EPSILON) {
            // Heuristic: if customer provides no reduction in total cost, can be pruned
            return true;
        }
        // Branching pruning rules
        for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(customer, List.of())) {
            int start = branch.getEdge().getStart();
            int end = branch.getEdge().getEnd();
            if (branch.getBound() == 1 && label.forbidsEdge(start, end)) {
                return true;
            } else if (branch.getBound() == 0 && label.containsEdge(start, end)) {
                return true;
            }
        }
        return labelDump.dominates(label);
    }

    private boolean isNodeUnreachable(Label label, Label previousLabel, LabelDump labelDump) {
        // returns true if node is infeasible or dominated
        if (previousLabel.isNodeVisited(label.node())) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
            return true;
        }

        for (int customer : Utils.bitSetToIntSet(label.visitedCustomers())) {
            for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(customer, List.of())) {
                int start = branch.getEdge().getStart();
                int end = branch.getEdge().getEnd();
                if (branch.getBound() == 1 && start != previousLabel.node() && end == label.node()) {
                    return true;
                } else if (branch.getBound() == 1 && start == previousLabel.node() && end != label.node()) {
                    return true;
                } else if (branch.getBound() == 0 && start == previousLabel.node() && end == label.node()) {
                    return true;
                }
            }
        }

        return labelDump.dominates(label);
    }

    private Label subtractBranchingDuals(Label currentLabel) {
        double updatedCost = currentLabel.cost();
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            if (currentLabel.containsEdge(branch.getEdge().getStart(), branch.getEdge().getEnd()) &&
                    currentLabel.visitedCustomers().get(branch.getCustomer())) {
                updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
            }
        }
        return new Label(currentLabel.demand(), updatedCost, currentLabel.node(), currentLabel.visitedNodes(),
                currentLabel.visitedCustomers(), currentLabel.parent());
    }

    private void monoDirectionalBacktracking(Stopwatch stopwatch) {
        Label root = Label.getRootLabel(graph.getStart(), graph.getSize(), -rmpSolution.getVehiclesDual());
        PriorityQueue<Label> queue = new PriorityQueue<>(64, Comparator.comparingDouble(Label::cost));
        labelDump.addLabel(root);
        queue.add(root);
        while (!queue.isEmpty()) {
            labelsProcessed++;
            if (stopwatch.timedOut()) {
                return;
            }
            Label currentLabel = queue.remove();
            if (stopEarly && currentLabel.node() == graph.getEnd() && currentLabel.cost() < -EPSILON) {
                solutionsFound++;
                if (solutionsFound >= STOP_AFTER_N_SOLUTIONS) {
                    return;
                }
            }

            // Update reduced costs by subtracting branching duals
            if (currentLabel.node() == graph.getEnd()) {
                currentLabel = subtractBranchingDuals(currentLabel);
            }

            if (!labelDump.dominates(currentLabel)) {
                for (int customer : graph.getReverseNeighborhood(currentLabel.node())) {
                    Label nextLabel = extendCustomer(currentLabel, customer);
                    if (!isCustomerUnreachable(nextLabel, customer, currentLabel, labelDump)) {
                        labelDump.addLabel(nextLabel);
                        queue.add(nextLabel);
                    }
                }
                for (int nextNode : graph.getAdjacentNodes(currentLabel.node())) {
                    Label nextLabel = extendNode(currentLabel, nextNode);
                    if (!isNodeUnreachable(nextLabel, currentLabel, labelDump)) {
                        labelDump.addLabel(nextLabel);
                        queue.add(nextLabel);
                    }
                }
            }
        }
    }

    public int getLabelsProcessed() {
        return labelsProcessed;
    }
}
