package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;

public class LabelSettingAlgorithm {

    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final Map<Integer, Double> dualValues;

    public LabelSettingAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.graph = new ESPPRCGraph(instance);
        this.dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
    }

    private List<FeasiblePath> translateLabelsToPaths(List<Label> negativeReducedCostLabels) {
        List<FeasiblePath> ret = new ArrayList<>();
        for (Label currentLabel : negativeReducedCostLabels) {
            Deque<Integer> nodes = new LinkedList<>();
            nodes.addFirst(currentLabel.node());

            FeasiblePath path = new FeasiblePath();
            path.addCustomers(Utils.boolArrayToIntSet(currentLabel.visitedCustomers()));

            Label lastLabel = currentLabel;
            currentLabel = currentLabel.parent();
            while (currentLabel != null) {
                if (lastLabel.node() != currentLabel.node()) {
                    nodes.addFirst(currentLabel.node());
                }
                lastLabel = currentLabel;
                currentLabel = currentLabel.parent();
            }
            int size = nodes.size();
            int lastNode = nodes.remove();
            assert lastNode == instance.getDepot();
            assert lastNode == graph.getStart();
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
        boolean[] updatedVisited = label.visitedCustomers();
        updatedVisited[customer] = true;
        return new Label(updatedDemand, updatedCost, label.node(), label.visitedNodes(), updatedVisited, label);

    }

    private Label extendNode(Label label, int nextNode) {
        double updatedCost = label.cost() + graph.getWeight(label.node(), nextNode);
        boolean[] updatedVisited = label.visitedNodes();
        updatedVisited[nextNode] = true;
        return new Label(label.demand(), updatedCost, nextNode, updatedVisited, label.visitedCustomers(), label);
    }

    private boolean isCustomerUnreachable(Label label, int customer, Label previousLabel, LabelDump labelDump) {
        if (previousLabel.isCustomerVisited(customer)) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
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

    private record Label(int demand, double cost, int node, boolean[] visitedNodes, boolean[] visitedCustomers,
                         LabelSettingAlgorithm.Label parent) implements Comparable<Label> {

        public static Label getRootLabel(int startNode, int numberOfNodes, double cost) {
            boolean[] visitedNodes = new boolean[numberOfNodes];
            boolean[] visitedCustomers = new boolean[numberOfNodes];
            Arrays.fill(visitedNodes, false);
            visitedNodes[startNode] = true;
            Arrays.fill(visitedCustomers, false);
            return new Label(0, cost, startNode, visitedNodes, visitedCustomers, null);
        }

        public boolean isNodeVisited(int node) {
            return visitedNodes[node];
        }

        public boolean isCustomerVisited(int customer) {
            return visitedCustomers[customer];
        }

        @Override
        public boolean[] visitedNodes() {
            return Arrays.copyOf(visitedNodes, visitedNodes.length);
        }

        @Override
        public boolean[] visitedCustomers() {
            return Arrays.copyOf(visitedCustomers, visitedCustomers.length);
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

        private final PrefixTreeMap<PrefixTreeMap<Label>> dump;

        public LabelDump() {
            dump = new PrefixTreeMap<>(graph.getSize());
        }

        public void addLabel(Label l) {
            PrefixTreeMap<Label> prefixTreeMap = dump.contains(l.visitedNodes) ? dump.get(l.visitedNodes) :
                    new PrefixTreeMap<>(graph.getSize());
            prefixTreeMap.insert(l.visitedCustomers, l);
            dump.insert(l.visitedNodes, prefixTreeMap);
        }

        public boolean dominates(Label l) {
            for (PrefixTreeMap<Label> p : dump.getValuesAtAllPrefixes(l.visitedNodes)) {
                for (Label other : p.getValuesAtAllPrefixes(l.visitedCustomers)) {
                    if (other.cost() <= l.cost()) {
                        return true;
                    }
                }
            }
            return false;
        }

        public List<Label> getNegativeReducedCostLabels(int node) {
            List<Label> ret = new ArrayList<>();
            for (PrefixTreeMap<Label> p :dump.getAllValues()){
                for(Label l : p.getAllValues()){
                    if(l.node() == node && l.cost() < -EPSILON){
                        ret.add(l);
                    }
                }
            }
            return ret;
        }
    }

}
