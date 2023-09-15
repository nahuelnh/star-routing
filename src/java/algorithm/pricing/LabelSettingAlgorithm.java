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
    private final int endNode;
    private final int startNode;
    private final int numberOfNodes;
    private final List<List<Integer>> graph;

    public LabelSettingAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.numberOfNodes = instance.getNumberOfNodes() + 1;
        this.startNode = instance.getDepot();
        this.endNode = numberOfNodes - 1;
        this.graph = createGraph();

    }

    private List<List<Integer>> createGraph() {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            graph.add(new ArrayList<>());
        }
        for (int i = 0; i < numberOfNodes; i++) {
            for (int j = 0; j < numberOfNodes; j++) {
                if (i != j && i != endNode && j != startNode && !(i == startNode && j == endNode)) {
                    graph.get(i).add(j);
                }
            }
        }
        return graph;
    }

    private List<FeasiblePath> computePathFromSolution(List<Label> negativeReducedCostLabels) {
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
            assert lastNode == startNode;
            for (int j = 1; j < size; j++) {
                int currentNode = nodes.remove();
                if (currentNode == endNode) {
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
        return computePathFromSolution(negativeReducedCostLabels);
    }

    private Label extendCustomer(Label label, int customer) {
        int updatedDemand = label.demand() + instance.getDemand(customer);
        double updatedCost = label.cost() - rmpSolution.getCustomerDual(instance.getCustomerIndex(customer));
        boolean[] updatedVisited = label.visitedCustomers();
        updatedVisited[customer] = true;
        return new Label(updatedDemand, updatedCost, label.node(), customer, label.visitedNodes(), updatedVisited,
                label);

    }

    private Label extendNode(Label label, int nextNode) {
        int fakeNode = nextNode == endNode ? instance.getDepot() : nextNode;
        double updatedCost = label.cost() + instance.getEdgeWeight(label.node(), fakeNode);
        boolean[] updatedVisited = label.visitedNodes();
        updatedVisited[nextNode] = true;
        return new Label(label.demand(), updatedCost, nextNode, -1, updatedVisited, label.visitedCustomers(), label);
    }

    private boolean isCustomerUnreachable(Label label, Label previousLabel, LabelStore labelStore) {
        if (previousLabel.isCustomerVisited(label.customer())) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
            return true;
        }
        return labelStore.isDominated(label);
    }

    private boolean isNodeUnreachable(Label label, Label previousLabel, LabelStore labelStore) {
        // returns true if node is infeasible or dominated
        if (previousLabel.isNodeVisited(label.node())) {
            return true;
        }
        if (label.demand() > instance.getCapacity()) {
            return true;
        }
        return labelStore.isDominated(label);
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
        for (int i = 0; i < numberOfNodes; i++) {
            reverseNeighborhoods.put(i, computeReverseNeighborhood(i));
        }
        LabelStore labels = new LabelStore(numberOfNodes, instance.getCapacity() + 1);
        Label root = Label.getRootLabel(startNode, numberOfNodes);
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
            for (int nextNode : graph.get(currentLabel.node())) {
                Label nextLabel = extendNode(currentLabel, nextNode);
                if (!isNodeUnreachable(nextLabel, currentLabel, labels)) {
                    labels.addLabel(nextLabel);
                    queue.add(nextLabel);
                }
            }
        }
        return labels.getNegativeReducedCostLabels(endNode, rmpSolution.getVehiclesDual());
    }

    private static class LabelStore {
        private final Label[][] labels;
        private final MinSegmentTree[] trees;

        public LabelStore(int numberOfNodes, int capacity) {
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

        public boolean isDominated(Label l) {
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
