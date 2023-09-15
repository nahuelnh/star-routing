package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
            Set<Integer> customers = new HashSet<>();
            Deque<Integer> nodes = new LinkedList<>();
            nodes.addFirst(currentLabel.node());
            customers.add(currentLabel.customer());

            Label lastLabel = currentLabel;
            currentLabel = currentLabel.parent();
            while (currentLabel != null) {
                if (lastLabel.node() != currentLabel.node()) {
                    nodes.addFirst(currentLabel.node());
                }
                customers.add(currentLabel.customer());
                lastLabel = currentLabel;
                currentLabel = currentLabel.parent();
            }
            customers.remove(-1);

            int size = nodes.size();
            FeasiblePath path = new FeasiblePath();
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
            path.addCustomers(customers);
            ret.add(path);
        }
        return ret;
    }

    public List<FeasiblePath> run() {
        List<Label> negativeReducedCostLabels = RighiniSalani();
        return translateLabelsToPaths(negativeReducedCostLabels);
    }

    private Label extendCustomer(Label label, int customer) {
        int updatedDemand = label.demand() + instance.getDemand(customer);
        double updatedCost = label.cost() - dualValues.get(customer);
        boolean[] updatedVisited = label.visitedCustomers();
        updatedVisited[customer] = true;
        return new Label(updatedDemand, updatedCost, label.node(), customer, label.visitedNodes(), updatedVisited,
                label);

    }

    private Label extendNode(Label label, int nextNode) {
        double updatedCost = label.cost() + graph.getWeight(label.node(), nextNode);
        boolean[] updatedVisited = label.visitedNodes();
        updatedVisited[nextNode] = true;
        return new Label(label.demand(), updatedCost, nextNode, -1, updatedVisited, label.visitedCustomers(), label);
    }

    private boolean isCustomerUnreachable(Label label, Label previousLabel, LabelDump labelStore) {
        if (previousLabel.isCustomerVisited(label.customer())) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
            return true;
        }
        return labelStore.dominates(label);
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

    private Set<Integer> computeReverseNeighborhood(int node) {
        Set<Integer> ret = new HashSet<>();
        for (int customer : instance.getCustomers()) {
            if (instance.getNeighbors(customer).contains(node)) {
                ret.add(customer);
            }
        }
        return ret;
    }

    // TODO optimize complexity
    private List<Label> RighiniSalani() {
        Map<Integer, Set<Integer>> reverseNeighborhoods = new HashMap<>();
        for (int i = 0; i < graph.getSize(); i++) {
            reverseNeighborhoods.put(i, computeReverseNeighborhood(i));
        }
        LabelDump labels = new LabelDump(graph.getSize(), instance.getCapacity() + 1);
        Label root = Label.getRootLabel(graph.getStart(), graph.getSize());
        labels.addLabel(root);
        PriorityQueue<Label> queue = new PriorityQueue<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Label currentLabel = queue.remove();
            for (int customer : reverseNeighborhoods.get(currentLabel.node())) {
                Label nextLabel = extendCustomer(currentLabel, customer);
                if (!isCustomerUnreachable(nextLabel, currentLabel, labels)) {
                    labels.addLabel(nextLabel);
                    queue.add(nextLabel);
                }
            }
            for (int nextNode : graph.getAdjacentNodes(currentLabel.node())) {
                Label nextLabel = extendNode(currentLabel, nextNode);
                if (!isNodeUnreachable(nextLabel, currentLabel, labels)) {
                    labels.addLabel(nextLabel);
                    queue.add(nextLabel);
                }
            }
        }
        return labels.getNegativeReducedCostLabels(graph.getEnd(), rmpSolution.getVehiclesDual());
    }

    private static class LabelDump {
        private final Label[][] labels;
        private final MinSegmentTree[] trees;

        public LabelDump(int numberOfNodes, int capacity) {
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

        public List<Label> getNegativeReducedCostLabels(int node, double maxCost) {
            List<Label> ret = new ArrayList<>();
            for (int q = 0; q < labels[node].length; q++) {
                if (labels[node][q] != null && labels[node][q].cost() - maxCost < -EPSILON) {
                    ret.add(labels[node][q]);
                }
            }
            return ret;
        }

    }

    private record Label(int demand, double cost, int node, int customer, boolean[] visitedNodes,
                         boolean[] visitedCustomers, LabelSettingAlgorithm.Label parent) implements Comparable<Label> {

        public static Label getRootLabel(int startNode, int numberOfNodes) {
            boolean[] visitedNodes = new boolean[numberOfNodes];
            boolean[] visitedCustomers = new boolean[numberOfNodes];
            Arrays.fill(visitedNodes, false);
            visitedNodes[startNode] = true;
            Arrays.fill(visitedCustomers, false);
            return new Label(0, 0, startNode, -1, visitedNodes, visitedCustomers, null);
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
            return "Label{" + "node=" + node + ", customer=" + customer + ", visitedNodes=" +
                    Arrays.toString(visitedNodes) + ", visitedCustomers=" + Arrays.toString(visitedCustomers) +
                    ", demand=" + demand + ", cost=" + cost + ", parent=" + parent + '}';
        }

        @Override
        public int compareTo(Label label) {
            if (this.node == label.node) {
                return 0;
            }
            return this.node < label.node ? 1 : -1;
        }
    }

}
