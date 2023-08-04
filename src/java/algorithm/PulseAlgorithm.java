package algorithm;

import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PulseAlgorithm {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final int endNode;
    private final int startNode;
    private final int numberOfNodes;
    private final List<Node> graph;
    private double[][] lowerBounds;
    private double bestSolutionFound;
    private List<List<Node>> foundPaths;
    private List<Set<Integer>> foundCustomerSets;

    public PulseAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.numberOfNodes = instance.getNumberOfNodes() + 1;
        this.startNode = instance.getDepot();
        this.endNode = numberOfNodes - 1;
        this.graph = createGraph();
    }

    public List<FeasiblePath> run() {
        lowerBounds = new double[numberOfNodes][instance.getCapacity() + 1];
        for (int i = 0; i < numberOfNodes; i++) {
            Arrays.fill(lowerBounds[i], Double.MIN_VALUE);
        }
        bestSolutionFound = Double.MAX_VALUE;
        foundPaths = new ArrayList<>();
        foundCustomerSets = new ArrayList<>();

        bound();
        bestSolutionFound = Double.MAX_VALUE;
        pulseWithNodeRule(graph.get(startNode), 0.0, new PartialPath(), true);
        return computePathsFromSolution();
    }

    private List<Node> createGraph() {
        List<Node> graph = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            graph.add(new Node(i));
        }
        for (int i = 0; i < numberOfNodes; i++) {
            for (int j = 0; j < numberOfNodes; j++) {
                if (i != j && i != endNode && j != startNode && !(i == startNode && j == endNode)) {
                    graph.get(i).addEdge(j);
                }
            }
        }
        return graph;
    }

    private List<FeasiblePath> computePathsFromSolution() {
        List<FeasiblePath> ret = new ArrayList<>();
        for (int i = 0; i < foundPaths.size(); i++) {
            List<Node> foundPath = foundPaths.get(i);
            Set<Integer> foundCustomers = foundCustomerSets.get(i);
            FeasiblePath path = new FeasiblePath();
            int lastNode = foundPath.get(0).getNumber();
            assert lastNode == instance.getDepot();
            assert lastNode == startNode;
            for (int j = 1; j < foundPath.size(); j++) {
                int currentNode = foundPath.get(j).getNumber();
                if (currentNode == endNode) {
                    currentNode = instance.getDepot();
                }
                path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
                lastNode = currentNode;
            }
            path.addCustomers(foundCustomers);
            ret.add(path);
        }
        return ret;
    }

    private boolean pruneWithNodeRule(Node currentNode, double edgeCost, PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand();
        double currentCost = visitedPath.getTotalCost() + edgeCost;
        if (!isFeasible(currentNode, currentDemand)) {
            return true;
        }
        if (checkBounds(currentNode, currentCost, currentDemand)) {
            return true;
        }
        if (rollback(currentNode, edgeCost, visitedPath)) {
            return true;
        }
        return false;
    }

    private boolean isNegativeReducedCost(double cost) {
        return cost - rmpSolution.getVehiclesDual() < -EPSILON;
    }

    private boolean pruneWithCustomerRule(Node currentNode, int deltaDemand, double deltaCost,
                                          PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand() + deltaDemand;
        double currentCost = visitedPath.getTotalCost() - deltaCost;
        if (deltaCost < EPSILON) {
            return true;
        }
        if (currentDemand > instance.getCapacity()) {
            return true;
        }
        if (checkBounds(currentNode, currentCost, currentDemand)) {
            return true;
        }
        return false;
    }

    private void propagate(Node currentNode, PartialPath visitedPath, boolean saveSolution) {
        if (currentNode.getNumber() == endNode) {
            if (visitedPath.getTotalCost() < bestSolutionFound) {
                bestSolutionFound = visitedPath.getTotalCost();
                if (saveSolution && isNegativeReducedCost(visitedPath.getTotalCost())) {
                    foundPaths.add(visitedPath.getCopyOfNodes());
                    foundCustomerSets.add(visitedPath.getCopyOfCustomers());
                }
            }
        } else {
            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node nextNode = graph.get(edge.getHead());
                pulseWithNodeRule(nextNode, edge.getCost(), visitedPath, saveSolution);
            }
            for (int nextCustomer : currentNode.getReverseNeighborhood()) {
                if (!visitedPath.isVisited(nextCustomer)) {
                    pulseWithCustomerRule(currentNode, nextCustomer, visitedPath, saveSolution);
                }
            }
        }
    }

    private void pulseWithNodeRule(Node currentNode, double edgeCost, PartialPath visitedPath, boolean saveSolution) {
        if (!pruneWithNodeRule(currentNode, edgeCost, visitedPath)) {
            currentNode.visit();
            visitedPath.addNode(currentNode, edgeCost);
            propagate(currentNode, visitedPath, saveSolution);
            currentNode.unvisit();
            visitedPath.removeLast(edgeCost);
        }
    }

    private void pulseWithCustomerRule(Node currentNode, int customer, PartialPath visitedPath, boolean saveSolution) {
        int deltaDemand = instance.getDemand(customer);
        double deltaCost = rmpSolution.getCustomerDual(instance.getCustomerIndex(customer));
        if (!pruneWithCustomerRule(currentNode, deltaDemand, deltaCost, visitedPath)) {
            visitedPath.addCustomer(customer, deltaDemand, deltaCost);
            propagate(currentNode, visitedPath, saveSolution);
            visitedPath.removeCustomer(customer, deltaDemand, deltaCost);
        }
    }

    private void bound() {
        for (int capacity = 0; capacity <= instance.getCapacity(); capacity++) {
            //for (int capacity = instance.getCapacity(); capacity >=0; capacity--) {
            for (Node node : graph) {
                bestSolutionFound = Double.MAX_VALUE;
                PartialPath partialPath = new PartialPath(0.0, capacity);
                pulseWithNodeRule(node, 0.0, partialPath, false);
                lowerBounds[node.getNumber()][capacity] = bestSolutionFound;
            }
        }
    }

    private boolean checkBounds(Node currentNode, double cost, int demand) {
        // Returns true if the branch is to be pruned
        if (lowerBounds[currentNode.getNumber()][demand] == Double.MAX_VALUE) {
            return true;
        }
        if (bestSolutionFound == Double.MAX_VALUE) {
            return false;
        }
        return cost + lowerBounds[currentNode.getNumber()][demand] >= bestSolutionFound;
    }

    private boolean isFeasible(Node currentNode, int demand) {
        return !currentNode.isVisited() && demand <= instance.getCapacity();
    }

    private boolean rollback(Node currentNode, double edgeCost, PartialPath visitedPath) {
        int size = visitedPath.size();
        if (size <= 1) {
            return false;
        }
        double lastNodeCost = visitedPath.getNodeAt(size - 1).getActualCost();
        int secondLastNodeNumber = visitedPath.getNodeAt(size - 2).getNumber();
        int currentNodeNumber = currentNode.getNumber() == endNode ? instance.getDepot() : currentNode.getNumber();
        return lastNodeCost + edgeCost >= instance.getEdgeWeight(secondLastNodeNumber, currentNodeNumber);
    }

    private class Node {
        private final int number;
        private final Collection<Edge> outgoingEdges;
        private final Set<Integer> reverseNeighborhood;
        private boolean visited;
        private double actualCost;

        public Node(int number) {
            this.number = number;
            this.outgoingEdges = new TreeSet<>();
            this.reverseNeighborhood = computeReverseNeighborhood();
            this.visited = false;
            this.actualCost = 0.0;
        }

        public double getActualCost() {
            return actualCost;
        }

        private void addToEdgeCost(double cost) {
            actualCost += cost;
        }

        private void resetEdgeCost() {
            actualCost = 0.0;
        }

        private Set<Integer> computeReverseNeighborhood() {
            Set<Integer> ret = new HashSet<>();
            for (int customer : instance.getCustomers()) {
                if (instance.getNeighbors(customer).contains(number)) {
                    ret.add(customer);
                }
            }
            return ret;
        }

        public int getNumber() {
            return number;
        }

        public boolean isVisited() {
            return visited;
        }

        public void addEdge(int head) {
            outgoingEdges.add(new Edge(head, this.number));
        }

        public Collection<Edge> getOutgoingEdges() {
            return outgoingEdges;
        }

        public void visit() {
            visited = true;
        }

        public void unvisit() {
            visited = false;
        }

        public Set<Integer> getReverseNeighborhood() {
            return reverseNeighborhood;
        }

    }

    private class Edge implements Comparable<Edge> {
        private final int head;
        private final double cost;

        public Edge(int head, int tail) {
            this.head = head;
            int fakeHead = head == endNode ? instance.getDepot() : head;
            this.cost = instance.getEdgeWeight(tail, fakeHead);
        }

        public int getHead() {
            return head;
        }

        public double getCost() {
            return cost;
        }

        @Override
        public int compareTo(Edge e) {
            double objCost = e.cost;
            if (this.cost == objCost) {
                return 0;
            }
            return this.cost > objCost ? 1 : -1;

        }
    }

    private class PartialPath {
        private final Node[] nodes;
        private final boolean[] visitedCustomers;
        private int size;
        private double totalCost;
        private int totalDemand;

        public PartialPath() {
            this(0.0, 0);
        }

        public PartialPath(double totalCost, int totalDemand) {
            this.nodes = new Node[numberOfNodes];
            this.size = 0;
            this.totalCost = totalCost;
            this.totalDemand = totalDemand;
            this.visitedCustomers = new boolean[numberOfNodes];
            Arrays.fill(visitedCustomers, false);
        }

        public int size() {
            return size;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public int getTotalDemand() {
            return totalDemand;
        }

        public void addNode(Node node, double edgeCost) {
            node.addToEdgeCost(edgeCost);
            size++;
            nodes[size - 1] = node;
            totalCost += edgeCost;
        }

        public void removeLast(double edgeCost) {
            nodes[size - 1].resetEdgeCost();
            size--;
            totalCost -= edgeCost;
        }

        public List<Node> getCopyOfNodes() {
            return Arrays.stream(nodes, 0, size).toList();
        }

        public Set<Integer> getCopyOfCustomers() {
            Set<Integer> ret = new HashSet<>();
            for (int node = 0; node < numberOfNodes; node++) {
                if (visitedCustomers[node]) {
                    ret.add(node);
                }
            }
            return ret;
        }

        public void addCustomer(int customer, int deltaDemand, double deltaCost) {
            visitedCustomers[customer] = true;
            totalDemand += deltaDemand;
            totalCost -= deltaCost;
            nodes[size - 1].addToEdgeCost(-deltaCost);
        }

        public void removeCustomer(int customer, int deltaDemand, double deltaCost) {
            visitedCustomers[customer] = false;
            totalDemand -= deltaDemand;
            totalCost += deltaCost;
            nodes[size - 1].addToEdgeCost(deltaCost);
        }

        public boolean isVisited(int customer) {
            return visitedCustomers[customer];
        }

        public Node getNodeAt(int index) {
            return nodes[index];
        }

    }

}
