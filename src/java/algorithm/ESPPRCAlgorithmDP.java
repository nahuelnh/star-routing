package algorithm;

import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
                if (i != j && i != endNode && j != startNode) {
                    graph.get(i).add(j);
                }
            }
        }
        return graph;
    }

    public List<FeasiblePath> run() {
        //BellmanFord();
        Label bestLabel = RighiniSalani();
        return new ArrayList<>();
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
        double updatedCost = label.getCost() - rmpSolution.getCustomerDual(customer);
        boolean[] updatedVisited = label.getVisitedCustomers();
        updatedVisited[customer] = true;
        return Optional.of(new Label(updatedDemand, updatedCost, label.getNode(), customer, label.getVisitedNodes(),
                updatedVisited));

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
        return Optional.of(new Label(label.getDemand(), updatedCost, nextNode, label.getCustomer(), updatedVisited,
                label.getVisitedCustomers()));
    }

    private void EFF(Label[][] labels, Label nextLabel) {
        //TODO implement segment tree to search best demand found
        if (labels[nextLabel.getNode()][nextLabel.getDemand()] == null ||
                !dominates(labels[nextLabel.getNode()][nextLabel.getDemand()], nextLabel)) {
            labels[nextLabel.getNode()][nextLabel.getDemand()] = nextLabel;
        }
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

    // TODO optimize complexity, keep extended labels, think how to make it DP
    // TODO process customers
    private Label RighiniSalani() {

        Map<Integer, Set<Integer>> reverseNeighborhoods = new HashMap<>();
        for (int i = 0; i < numberOfNodes; i++) {
            reverseNeighborhoods.put(i, computeReverseNeighborhood(i));
        }

        Label[][] labels = new Label[numberOfNodes][instance.getCapacity() + 1];
        labels[startNode][0] = Label.getRootLabel(startNode, numberOfNodes, instance.getNumberOfCustomers());
        // TODO use priority queue to process one node at a time
        Queue<Label> queue = new LinkedList<>();
        queue.add(labels[startNode][0]);
        while (!queue.isEmpty()) {
            Label currentLabel = queue.remove();
            for (int customer : reverseNeighborhoods.get(currentLabel.getNode())) {
                Optional<Label> nextLabel = extendCustomer(currentLabel, customer);
                if (nextLabel.isPresent()) {
                    EFF(labels, nextLabel.get());
                    queue.add(nextLabel.get());
                }
            }
            for (int nextNode : graph.get(currentLabel.getNode())) {
                Optional<Label> nextLabel = extendNode(currentLabel, nextNode);
                if (nextLabel.isPresent()) {
                    EFF(labels, nextLabel.get());
                    queue.add(nextLabel.get());
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

    private static class Label {
        private final int node;
        private final int customer;
        private final boolean[] visitedNodes;
        private final boolean[] visitedCustomers;
        private final int demand;
        private final double cost;

        public Label(int demand, double cost, int node, int customer, boolean[] visitedNodes,
                     boolean[] visitedCustomers) {
            this.demand = demand;
            this.cost = cost;
            this.node = node;
            this.customer = customer;
            this.visitedNodes = visitedNodes;
            this.visitedCustomers = visitedCustomers;
        }

        public static Label getRootLabel(int startNode, int numberOfNodes, int numberOfCustomers) {
            boolean[] visitedNodes = new boolean[numberOfNodes];
            boolean[] visitedCustomers = new boolean[numberOfCustomers];
            Arrays.fill(visitedNodes, false);
            visitedNodes[startNode] = true;
            Arrays.fill(visitedCustomers, false);
            return new Label(0, 0, startNode, -1, visitedNodes, visitedCustomers);
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
    }

}
