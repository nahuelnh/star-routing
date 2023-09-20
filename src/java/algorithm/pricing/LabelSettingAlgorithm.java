package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LabelSettingAlgorithm {

    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final Map<Integer, Double> dualValues;
    private final boolean applyFakeCostHeuristic;
    private final double alpha;
    private final LabelDump labelDump;

    public LabelSettingAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution,
                                 boolean applyRelaxedDominance, boolean applyFakeCostHeuristic) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        this.graph = new ESPPRCGraph(instance);

        // Heuristic: sort decreasingly by benefit/cost
        this.graph.sortReverseNeighborhoods(
                Comparator.comparing(s -> dualValues.containsKey(s) ? -dualValues.get(s) / instance.getDemand(s) : 0));

        this.applyFakeCostHeuristic = applyFakeCostHeuristic;
        this.alpha = computeCostFactor();
        if (applyRelaxedDominance) {
            this.labelDump = new RelaxedLabelDump(graph.getSize(), instance.getCapacity() + 1);
        } else {
            this.labelDump = new StrictLabelDump(graph.getSize());
        }
    }

    public LabelSettingAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this(instance, rmpSolution, false, false);
    }

    private double computeCostFactor() {
        int sum = 0;
        for (int i = 0; i < graph.getSize(); i++) {
            for (int j = 0; j < graph.getSize(); j++) {
                if (graph.edgeExists(i, j)) {
                    sum += graph.getWeight(i, j);
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

    public List<FeasiblePath> run() {
        monoDirectionalBacktracking();
        return getNegativeReducedCostPaths();
    }

    private double getLittleFakeCost(Label label, int customer) {
        if (graph.edgeExists(customer, label.node())) {
            return graph.getWeight(customer, label.node()) * alpha;
        }
        return 0.0;
    }

    private Label extendCustomer(Label label, int customer) {
        int updatedDemand = label.demand() + instance.getDemand(customer);
        double updatedCost = label.cost() - dualValues.get(customer);
        if (applyFakeCostHeuristic) {
            updatedCost += getLittleFakeCost(label, customer);
        }
        BitSet updatedVisited = label.copyOfVisitedCustomers();
        updatedVisited.set(customer);
        return new Label(updatedDemand, updatedCost, label.node(), label.copyOfVisitedNodes(), updatedVisited, label);

    }

    private Label extendNode(Label label, int nextNode) {
        double updatedCost = label.cost() + graph.getWeight(label.node(), nextNode);
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
        return labelDump.dominates(label);
    }

    private void monoDirectionalBacktracking() {

        Label root = Label.getRootLabel(graph.getStart(), graph.getSize(), -rmpSolution.getVehiclesDual());
        labelDump.addLabel(root);

        PriorityQueue<Label> queue = new PriorityQueue<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Label currentLabel = queue.remove();
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

    private static class RelaxedLabelDump implements LabelDump {
        private final Label[][] labels;
        private final MinSegmentTree[] trees;

        public RelaxedLabelDump(int numberOfNodes, int capacity) {
            this.labels = new Label[numberOfNodes][capacity + 1];
            this.trees = new MinSegmentTree[numberOfNodes];
            double[] arr = new double[capacity + 1];
            Arrays.fill(arr, Double.MAX_VALUE);
            for (int i = 0; i < numberOfNodes; i++) {
                trees[i] = new MinSegmentTree(arr);
            }
        }

        @Override
        public void addLabel(Label l) {
            labels[l.node()][l.demand()] = l;
            trees[l.node()].update(l.demand(), l.cost());
        }

        @Override
        public boolean dominates(Label l) {
            return trees[l.node()].query(0, l.demand() + 1) < l.cost();
        }

        @Override
        public List<Label> getNegativeReducedCostLabels(int node) {
            List<Label> ret = new ArrayList<>();
            for (int q = 0; q < labels[node].length; q++) {
                if (labels[node][q] != null && labels[node][q].cost() < -EPSILON) {
                    ret.add(labels[node][q]);
                }
            }
            return ret;
        }
    }

    private static class StrictLabelDump implements LabelDump {

        private final List<Map<BitSet, Map<BitSet, Label>>> dump;

        public StrictLabelDump(int size) {
            dump = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                dump.add(new HashMap<>());
            }
        }

        @Override
        public void addLabel(Label l) {
            Map<BitSet, Map<BitSet, Label>> currentNodeMap = dump.get(l.node());
            currentNodeMap.putIfAbsent(l.visitedNodes(), new HashMap<>());
            currentNodeMap.get(l.visitedNodes()).put(l.visitedCustomers(), l);

        }

        @Override
        public boolean dominates(Label l) {
            for (BitSet visitedNodes : dump.get(l.node()).keySet()) {
                if (Utils.isSubset(visitedNodes, l.visitedNodes())) {
                    Map<BitSet, Label> map = dump.get(l.node()).get(visitedNodes);
                    for (BitSet visitedCustomers : map.keySet()) {
                        if (Utils.isSubset(visitedCustomers, l.visitedCustomers())) {
                            if (map.get(visitedCustomers).cost() <= l.cost()) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public List<Label> getNegativeReducedCostLabels(int node) {
            List<Label> ret = new ArrayList<>();
            for (BitSet visitedNodes : dump.get(node).keySet()) {
                Map<BitSet, Label> map = dump.get(node).get(visitedNodes);
                for (BitSet visitedCustomers : map.keySet()) {
                    if (map.get(visitedCustomers).cost() < -EPSILON) {
                        ret.add(map.get(visitedCustomers));
                    }
                }
            }
            return ret;
        }
    }
}
