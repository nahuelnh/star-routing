package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnVisitFlow;
import commons.FeasiblePath;
import commons.Graph;
import commons.Instance;
import commons.Stopwatch;

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
    private final boolean stopEarly = false;
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
        this.graph = new ESPPRCGraph(instance, rmpSolution);
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


    private List<FeasiblePath> getNegativeReducedCostPaths() {
        List<FeasiblePath> ret = new ArrayList<>();
        for (Label currentLabel : labelDump.getNegativeReducedCostLabels(graph.getSink().getId())) {
            ret.add(currentLabel.translateToFeasiblePath(graph));
        }
        return ret;
    }

    public List<FeasiblePath> run(Duration timeLimit) {
        Stopwatch stopwatch = new Stopwatch(timeLimit);
        monoDirectionalBacktracking(stopwatch);
        return getNegativeReducedCostPaths();
    }


    private Label extendNode(Label label, Graph.ContinuousWeightedEdge<ESPPRCGraph.ESPPRCNode> edge) {
        int nextNode = edge.getEnd().getSrNodeId();
        int nextCustomer = edge.getEnd().getCustomer();

        // Update values
        double updatedCost = label.cost() + edge.getWeight();
        int updatedDemand = label.demand() + instance.getDemand(nextCustomer);
        BitSet updatedVisited = label.copyOfVisitedNodes();
        updatedVisited.set(nextNode);
        BitSet updatedVisitedCustomers = label.copyOfVisitedCustomers();
        updatedVisitedCustomers.set(nextCustomer);

        // Subtract branching dual variables
        for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(nextCustomer, List.of())) {
            if (label.containsEdge(branch.getStartNode().getId(), branch.getStartNode().getId())) {
                updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
            }
        }

        return new Label(edge.getEnd(), updatedCost, updatedDemand, updatedVisited, updatedVisitedCustomers, label);
    }

    private boolean isCustomerUnreachable(Label label, int customer, Label previousLabel, LabelDump labelDump) {


        return labelDump.dominates(label);
    }

    private boolean isNodeUnreachable(Label nextLabel, Label currentLabel, LabelDump labelDump) {
        // returns true if node is infeasible or dominated
        int nextNode = nextLabel.node().getSrNodeId();
        int nextCustomer = nextLabel.node().getCustomer();

        if (currentLabel.isNodeVisited(nextNode)) {
            return true;
        }
        if (nextLabel.demand() > instance.getCapacity()) {
            return true;
        }
        if (currentLabel.isCustomerVisited(nextCustomer)) {
            return true;
        }
        if (dualValues.get(nextCustomer) < EPSILON) {
            // Heuristic: if customer provides no reduction in total cost, can be pruned
            return true;
        }

        // Branching pruning rules
        for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(nextCustomer, List.of())) {
            int start = branch.getStartNode().getId();
            int end = branch.getEndNode().getId();
            if (branch.getBound() == 1 && nextLabel.forbidsEdge(start, end)) {
                return true;
            } else if (branch.getBound() == 0 && nextLabel.containsEdge(start, end)) {
                return true;
            }
        }

        // Branching pruning rules
        for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
            if (nextLabel.isCustomerVisited(branch.getCustomer())) {
                int start = branch.getStartNode().getId();
                int end = branch.getEndNode().getId();
                if (branch.getBound() == 1 && start != currentLabel.node().getSrNodeId() && end == nextLabel.node().getSrNodeId()) {
                    return true;
                } else if (branch.getBound() == 1 && start == currentLabel.node().getSrNodeId() && end != nextLabel.node().getSrNodeId()) {
                    return true;
                } else if (branch.getBound() == 0 && start == currentLabel.node().getSrNodeId() && end == nextLabel.node().getSrNodeId()) {
                    return true;
                }
            }
        }

        return labelDump.dominates(nextLabel);
    }

    private double getInitialCost() {
        double initialCost = -rmpSolution.getVehiclesDual();
        for (double fleetSizeDual : rmpSolution.getFleetSizeDuals()) {
            initialCost -= fleetSizeDual;
        }
        return initialCost;
    }

    private void monoDirectionalBacktracking(Stopwatch stopwatch) {
        Label root = Label.getRootLabel(graph.getSource(), graph.getSize(), getInitialCost());
        PriorityQueue<Label> queue = new PriorityQueue<>(64, Comparator.comparingDouble(Label::cost));
        labelDump.addLabel(root);
        queue.add(root);
        while (!queue.isEmpty()) {
            labelsProcessed++;
            if (stopwatch.timedOut()) {
                return;
            }
            Label currentLabel = queue.remove();
            if (stopEarly && currentLabel.node().getId() == graph.getSink().getId() && currentLabel.cost() < -EPSILON) {
                solutionsFound++;
                if (solutionsFound >= STOP_AFTER_N_SOLUTIONS) {
                    return;
                }
            }
            if (!labelDump.dominates(currentLabel)) {
                for (Graph.Edge<ESPPRCGraph.ESPPRCNode> edge : graph.getIncidentEdges(currentLabel.node().getId())) {
                    Label nextLabel = extendNode(currentLabel, (Graph.ContinuousWeightedEdge<ESPPRCGraph.ESPPRCNode>) edge);
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
