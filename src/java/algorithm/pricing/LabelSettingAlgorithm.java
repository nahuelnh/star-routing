package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LabelSettingAlgorithm {

    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final Map<Integer, Double> dualValues;

    public LabelSettingAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        this.graph = new ESPPRCGraph(instance, dualValues);
    }

    private Deque<Integer> getNodesFromLabelPath(Label currentLabel) {
        Deque<Integer> nodes = new LinkedList<>();
        nodes.addFirst(currentLabel.node());
        Label lastLabel = currentLabel;
        currentLabel = currentLabel.parent();
        while (currentLabel != null) {
            if (lastLabel.node() != currentLabel.node()) {
                nodes.addFirst(currentLabel.node());
            }
            lastLabel = currentLabel;
            currentLabel = currentLabel.parent();
        }
        return nodes;
    }

    private List<FeasiblePath> translateLabelsToPaths(List<Label> negativeReducedCostLabels) {
        List<FeasiblePath> ret = new ArrayList<>();
        for (Label currentLabel : negativeReducedCostLabels) {
            FeasiblePath path = new FeasiblePath();
            path.addCustomers(Utils.bitSetToIntSet(currentLabel.visitedCustomers()));
            Deque<Integer> nodes = getNodesFromLabelPath(currentLabel);
            int size = nodes.size();
            int lastNode = nodes.remove();
            for (int j = 1; j < size; j++) {
                int currentNode = nodes.remove();
                if (currentNode == graph.getEnd()) {
                    currentNode = instance.getDepot();
                }
                path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
                lastNode = currentNode;
            }
            ret.add(path);
        }
        return ret;
    }

    public List<FeasiblePath> run() {
        List<Label> negativeReducedCostLabels = monoDirectionalBacktracking();
        return translateLabelsToPaths(negativeReducedCostLabels);
    }

    private Label extendCustomer(Label label, int customer) {
        int updatedDemand = label.demand() + instance.getDemand(customer);
        double updatedCost = label.cost() - dualValues.get(customer);
        BitSet updatedVisited = label.visitedCustomers();
        updatedVisited.set(customer);
        return new Label(updatedDemand, updatedCost, label.node(), label.visitedNodes(), updatedVisited, label);

    }

    private Label extendNode(Label label, int nextNode) {
        double updatedCost = label.cost() + graph.getWeight(label.node(), nextNode);
        BitSet updatedVisited = label.visitedNodes();
        updatedVisited.set(nextNode);
        return new Label(label.demand(), updatedCost, nextNode, updatedVisited, label.visitedCustomers(), label);
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

    private List<Label> monoDirectionalBacktracking() {
        //RelaxedLabelDump labelDump = new RelaxedLabelDump(graph.getSize(), instance.getCapacity() + 1);
        LabelDump labelDump = new LabelDump();
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
        return labelDump.getNegativeReducedCostLabels(graph.getEnd());
    }

    private static class RelaxedLabelDump {
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

        public void addLabel(Label l) {
            labels[l.node()][l.demand()] = l;
            trees[l.node()].update(l.demand(), l.cost());
        }

        public boolean dominates(Label l) {
            return trees[l.node()].query(0, l.demand() + 1) < l.cost();
        }

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

    private record Label(int demand, double cost, int node, BitSet visitedNodes, BitSet visitedCustomers,
                         LabelSettingAlgorithm.Label parent) implements Comparable<Label> {

        public static Label getRootLabel(int startNode, int numberOfNodes, double cost) {
            BitSet visitedNodes = new BitSet(numberOfNodes);
            BitSet visitedCustomers = new BitSet(numberOfNodes);
            visitedNodes.set(startNode);
            return new Label(0, cost, startNode, visitedNodes, visitedCustomers, null);
        }

        public boolean isNodeVisited(int node) {
            return visitedNodes.get(node);
        }

        public boolean isCustomerVisited(int customer) {
            return visitedCustomers.get(customer);
        }

        @Override
        public BitSet visitedNodes() {
            return visitedNodes.get(0, visitedNodes.length());
        }

        @Override
        public BitSet visitedCustomers() {
            return visitedCustomers.get(0, visitedCustomers.length());
        }

        @Override
        public String toString() {
            return "Label{" + "demand=" + demand + ", cost=" + cost + ", node=" + node + ", parent=" +
                    (parent == null ? null : parent.node) + '}';
        }

        @Override
        public int compareTo(Label label) {
            if (this.node == label.node) {
                return 0;
            }
            return this.node < label.node ? 1 : -1;
        }
    }

    private class LabelDump {

        private final List<Map<BitSet, Map<BitSet, Label>>> dump;

        public LabelDump() {
            dump = new ArrayList<>(graph.getSize());
            for (int i = 0; i < graph.getSize(); i++) {
                dump.add(new HashMap<>());
            }
        }

        public void addLabel(Label l) {
            Map<BitSet, Map<BitSet, Label>> currentNodeMap = dump.get(l.node());
            currentNodeMap.putIfAbsent(l.visitedNodes, new HashMap<>());
            currentNodeMap.get(l.visitedNodes).put(l.visitedCustomers, l);

        }

        public boolean dominates(Label l) {
            for (BitSet visitedNodes : dump.get(l.node()).keySet()) {
                if (Utils.isSubset(visitedNodes, l.visitedNodes)) {
                    Map<BitSet, Label> map = dump.get(l.node()).get(visitedNodes);
                    for (BitSet visitedCustomers : map.keySet()) {
                        if (Utils.isSubset(visitedCustomers, l.visitedCustomers)) {
                            if (map.get(visitedCustomers).cost() <= l.cost()) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

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
