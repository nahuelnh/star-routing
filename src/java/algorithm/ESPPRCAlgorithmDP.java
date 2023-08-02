package algorithm;

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
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class ESPPRCAlgorithmDP {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final int endNode;
    private final int startNode;
    private final int numberOfNodes;
    private final List<List<Integer>> graph;

    public ESPPRCAlgorithmDP(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.numberOfNodes = instance.getNumberOfNodes() + 1;
        this.startNode = instance.getDepot();
        this.endNode = numberOfNodes - 1;
        this.graph = createGraph();

    }

    private boolean dominates(Label former, Label latter) {
        return former.getCost() <= latter.getCost();
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

    private FeasiblePath computePathFromSolution(Label bestLabel) {
        Set<Integer> customers = new HashSet<>();
        Deque<Integer> nodes = new LinkedList<>();
        while (bestLabel != null) {
            nodes.addFirst(bestLabel.getNode());
            customers.add(bestLabel.getCustomer());
            bestLabel = bestLabel.getParent();
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
        return path;
    }

    public List<FeasiblePath> run() {
        FeasiblePath feasiblePath = new FeasiblePath();
        Label bestLabel = RighiniSalani();
        return List.of(computePathFromSolution(bestLabel));
    }

    private Optional<Label> extendCustomer(Label label, int customer) {
        // Returns empty if node is unreachable
        if (label.isCustomerVisited(customer)) {
            return Optional.empty();
        }
        int updatedDemand = label.getDemand() + instance.getDemand(customer);
        if (updatedDemand >= instance.getCapacity()) {
            return Optional.empty();
        }
        double updatedCost = label.getCost() - rmpSolution.getCustomerDual(instance.getCustomerIndex(customer));
        boolean[] updatedVisited = label.getVisitedCustomers();
        updatedVisited[customer] = true;
        return Optional.of(new Label(updatedDemand, updatedCost, label.getNode(), customer, label.getVisitedNodes(),
                updatedVisited, label));

    }

    private Optional<Label> extendNode(Label label, int nextNode) {
        // Returns empty if node is unreachable
        if (label.isNodeVisited(nextNode)) {
            return Optional.empty();
        }
        if (label.getDemand() >= instance.getCapacity()) {
            return Optional.empty();
        }
        int fakeNode = nextNode == endNode ? instance.getDepot() : nextNode;
        double updatedCost = label.getCost() + instance.getEdgeWeight(label.getNode(), fakeNode);

        boolean[] updatedVisited = label.getVisitedNodes();
        updatedVisited[nextNode] = true;
        return Optional.of(
                new Label(label.getDemand(), updatedCost, nextNode, -1, updatedVisited, label.getVisitedCustomers(),
                        label));
    }

    //    private boolean EFF(Label[][] labels, Label nextLabel) {
    //        //TODO implement segment tree to search best demand found
    //        if (labels[nextLabel.getNode()][nextLabel.getDemand()] == null ||
    //                !dominates(labels[nextLabel.getNode()][nextLabel.getDemand()], nextLabel)) {
    //            labels[nextLabel.getNode()][nextLabel.getDemand()] = nextLabel;
    //            return true;
    //        }
    //        return false;
    //    }

    private Set<Integer> computeReverseNeighborhood(int node) {
        Set<Integer> ret = new HashSet<>();
        for (int customer : instance.getCustomers()) {
            if (instance.getNeighbors(customer).contains(node)) {
                ret.add(customer);
            }
        }
        return ret;
    }

    private boolean EFF(Label[][] labels, Label nextLabel) {
        for (int q = 0; q <= nextLabel.getDemand(); q++) {
            if (labels[nextLabel.getNode()][q] != null &&
                    labels[nextLabel.getNode()][q].getCost() <= nextLabel.getCost()) {
                return false;
            }
        }
        return true;
    }

    // TODO optimize complexity
    private Label RighiniSalani() {

        Map<Integer, Set<Integer>> reverseNeighborhoods = new HashMap<>();
        for (int i = 0; i < numberOfNodes; i++) {
            reverseNeighborhoods.put(i, computeReverseNeighborhood(i));
        }
        Label[][] labels = new Label[numberOfNodes][instance.getCapacity() + 1];
        labels[startNode][0] = Label.getRootLabel(startNode, numberOfNodes);
        PriorityQueue<Label> queue = new PriorityQueue<>();
        queue.add(labels[startNode][0]);
        while (!queue.isEmpty()) {
            Label currentLabel = queue.remove();
            for (int customer : reverseNeighborhoods.get(currentLabel.getNode())) {
                Optional<Label> nextLabel = extendCustomer(currentLabel, customer);
                if (nextLabel.isPresent() && EFF(labels, nextLabel.get())) {
                    Label l = nextLabel.get();
                    labels[l.getNode()][l.getDemand()] = l;
                    queue.add(l);
                }
            }
            for (int nextNode : graph.get(currentLabel.getNode())) {
                Optional<Label> nextLabel = extendNode(currentLabel, nextNode);
                if (nextLabel.isPresent() && EFF(labels, nextLabel.get())) {
                    Label l = nextLabel.get();
                    labels[l.getNode()][l.getDemand()] = l;
                    queue.add(l);
                }
            }
        }
        Label bestLabel = null;
        for (int q = 0; q < instance.getCapacity() + 1; q++) {
            if (labels[endNode][q] != null) {
                if (bestLabel == null || bestLabel.getCost() < labels[endNode][q].getCost()) {
                    bestLabel = labels[endNode][q];
                }
            }
        }
        return bestLabel;
    }

    private static class Label implements Comparable<Label> {
        private final int node;
        private final int customer;
        private final boolean[] visitedNodes;
        private final boolean[] visitedCustomers;
        private final int demand;
        private final double cost;
        private final Label parent;

        public Label(int demand, double cost, int node, int customer, boolean[] visitedNodes,
                     boolean[] visitedCustomers, Label parent) {
            this.demand = demand;
            this.cost = cost;
            this.node = node;
            this.customer = customer;
            this.visitedNodes = visitedNodes;
            this.visitedCustomers = visitedCustomers;
            this.parent = parent;
        }

        public static Label getRootLabel(int startNode, int numberOfNodes) {
            boolean[] visitedNodes = new boolean[numberOfNodes];
            boolean[] visitedCustomers = new boolean[numberOfNodes];
            Arrays.fill(visitedNodes, false);
            visitedNodes[startNode] = true;
            Arrays.fill(visitedCustomers, false);
            return new Label(0, 0, startNode, -1, visitedNodes, visitedCustomers, null);
        }

        public int getDemand() {
            return demand;
        }

        public double getCost() {
            return cost;
        }

        public int getNode() {
            return node;
        }

        public boolean isNodeVisited(int node) {
            return visitedNodes[node];
        }

        public boolean isCustomerVisited(int customer) {
            return visitedCustomers[customer];
        }

        public boolean[] getVisitedNodes() {
            return Arrays.copyOf(visitedNodes, visitedNodes.length);
        }

        public boolean[] getVisitedCustomers() {
            return Arrays.copyOf(visitedCustomers, visitedCustomers.length);
        }

        public int getCustomer() {
            return customer;
        }

        public Label getParent() {
            return parent;
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
