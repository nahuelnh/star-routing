package algorithm;

import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PulseAlgorithm {

    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final double[][] lowerBounds;
    private final int endNode;
    private final int startNode;
    private final int numberOfNodes;
    private final List<Node> graph;
    private double bestSolutionFound;
    private List<Node> bestPath;

    public PulseAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;

        this.numberOfNodes = instance.getNumberOfNodes() + 1;
        this.startNode = instance.getDepot();
        this.endNode = instance.getNumberOfNodes();
        graph = new ArrayList<>();
        generateGraph();

        this.lowerBounds = new double[numberOfNodes][instance.getCapacity() + 1];
        for (int i = 0; i < numberOfNodes; i++) {
            Arrays.fill(lowerBounds[i], Double.MIN_VALUE);
        }
        this.bestSolutionFound = Double.MAX_VALUE;
        bestPath = new ArrayList<>();
    }

    private void generateGraph() {
        for (int i = 0; i < numberOfNodes; i++) {
            graph.add(new Node(i));
        }
        for (int i = 0; i < numberOfNodes; i++) {
            for (int j = 0; j < numberOfNodes; j++) {
                if (i != j && i != endNode && j != startNode) {
                    graph.get(i).addEdge(j);
                }
            }
        }
    }

    private FeasiblePath getPathFromSolution() {
        FeasiblePath path = new FeasiblePath();
        int lastNode = bestPath.get(0).getNumber();
        assert lastNode == instance.getDepot();
        assert lastNode == startNode;
        for (int i = 1; i < bestPath.size(); i++) {
            int currentNode = bestPath.get(i).getNumber();
            if (currentNode == endNode) {
                currentNode = instance.getDepot();
            }
            path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
            lastNode = currentNode;
        }
        return path;
    }

    public FeasiblePath getOptimalPath() {
        bound();
        bestSolutionFound = Double.MAX_VALUE;
        pulse(graph.get(startNode), new PartialPath());
        System.out.println(bestSolutionFound);
        System.out.println(getPathFromSolution());
        return getPathFromSolution();
    }

    private Set<Integer> getNewCustomers(Node currentNode, PartialPath visitedPath) {
        Set<Integer> ret = new HashSet<>(currentNode.getReverseNeighborhood());
        ret.removeAll(visitedPath.getVisitedCustomers());
        return ret;
    }

    private void pulse(Node currentNode, PartialPath visitedPath) {
        if (isFeasible(currentNode, visitedPath) && !checkBounds(currentNode, visitedPath) &&
                !rollback(currentNode, visitedPath)) {
            currentNode.visit();
            visitedPath.addNode(currentNode);
            if (currentNode.getNumber() == endNode) {
                if (visitedPath.getTotalCost() < bestSolutionFound) {
                    bestSolutionFound = visitedPath.getTotalCost();
                    bestPath = visitedPath.getCopyOfNodes();
                }
            } else {
                for (Edge edge : currentNode.getOutgoingEdges()) {
                    Node nextNode = graph.get(edge.getHead());
                    Set<Integer> newCustomers = getNewCustomers(currentNode, visitedPath);

                    double cost = nextNode.getCost();
                    cost -= newCustomers.stream().mapToDouble(rmpSolution::getCustomerDual).sum();
                    int demand = nextNode.getDemand();
                    demand += newCustomers.stream().mapToInt(instance::getDemand).sum();

                    nextNode.setCost(cost);
                    nextNode.setDemand(demand);
                    nextNode.setNewNeighbors(newCustomers);
                    pulse(nextNode, visitedPath);
                }
            }
            currentNode.unvisit();
            visitedPath.removeLast();
            //TODO remove new neighbors
        }
    }

    private void bound() {
        for (int capacity = instance.getCapacity(); capacity >= 0; capacity--) {
            for (Node node : graph) {
                bestSolutionFound = Double.MAX_VALUE;
                PartialPath partialPath = new PartialPath(0, capacity);
                pulse(node, partialPath);
                if (bestSolutionFound != Double.MAX_VALUE) {
                    lowerBounds[node.getNumber()][capacity] = bestSolutionFound;
                }
            }
        }
    }

    private boolean checkBounds(Node currentNode, PartialPath partialPath) {
        // Returns true if the branch is to be pruned
        int demand = partialPath.getTotalDemand() + currentNode.getDemand();
        double cost = partialPath.getTotalCost() + currentNode.getCost();
        if (lowerBounds[currentNode.getNumber()][demand] == Double.MAX_VALUE) {
            return true;
        }
        if (lowerBounds[currentNode.getNumber()][demand] == Double.MIN_VALUE) {
            return false;
        }
        if (bestSolutionFound == Double.MAX_VALUE) {
            return false;
        }
        return cost + lowerBounds[currentNode.getNumber()][demand] >= bestSolutionFound;
    }

    private boolean isFeasible(Node currentNode, PartialPath partialPath) {
        int demand = currentNode.getDemand() + partialPath.getTotalDemand();
        return !currentNode.isVisited() && demand <= instance.getCapacity();
    }

    private boolean rollback(Node currentNode, PartialPath visitedPath) {
        return false;
    }

    private class Node {
        private final int number;

        private final List<Edge> outgoingEdges;

        private final Set<Integer> reverseNeighborhood;
        private double cost;
        private int demand;
        private boolean visited;
        private Set<Integer> newNeighbors;

        public Node(int number) {
            this.number = number;
            this.cost = 0.0;
            this.outgoingEdges = new ArrayList<>();
            this.reverseNeighborhood = computeReverseNeighborhood();
            this.demand = reverseNeighborhood.stream().mapToInt(instance::getDemand).sum();
            this.visited = false;
            this.newNeighbors = new HashSet<>();
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

        public int getDemand() {
            return demand;
        }

        public void setDemand(int demand) {
            this.demand = demand;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public boolean isVisited() {
            return visited;
        }

        public void addEdge(int head) {
            outgoingEdges.add(new Edge(head, this.number));
        }

        public List<Edge> getOutgoingEdges() {
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

        public Set<Integer> getNewNeighbors() {
            return newNeighbors;
        }

        public void setNewNeighbors(Set<Integer> newNeighbors) {
            this.newNeighbors = newNeighbors;
        }
    }

    private class Edge {
        private final int head;
        private final int tail;
        private final double cost;

        public Edge(int head, int tail) {
            this.head = head;
            this.tail = tail;
            int fakeHead = head == endNode ? instance.getDepot() : head;
            this.cost = instance.getEdgeWeight(tail, fakeHead);
        }

        public int getHead() {
            return head;
        }

        public int getTail() {
            return tail;
        }

        public double getCost() {
            return cost;
        }

    }

    private class PartialPath {
        private final Deque<Node> nodes;
        private final Set<Integer> visitedCustomers;
        private double totalCost;
        private int totalDemand;

        public PartialPath() {
            this.nodes = new LinkedList<>();
            this.totalCost = 0;
            this.totalDemand = 0;
            this.visitedCustomers = new HashSet<>();
        }

        public PartialPath(double totalCost, int totalDemand) {
            this.nodes = new LinkedList<>();
            this.totalCost = totalCost;
            this.totalDemand = totalDemand;
            this.visitedCustomers = new HashSet<>();
        }

        public double getTotalCost() {
            return totalCost;
        }

        public int getTotalDemand() {
            return totalDemand;
        }

        public void addNode(Node node) {
            nodes.addLast(node);
            totalCost += node.cost;
            totalDemand += node.demand;
            visitedCustomers.addAll(node.getReverseNeighborhood());
        }

        public void removeLast() {
            Node last = nodes.getLast();
            visitedCustomers.removeAll(last.getNewNeighbors());
            nodes.removeLast();
            //TODO remove new neighbors
        }

        public List<Node> getCopyOfNodes() {
            return new ArrayList<>(nodes);
        }

        public Set<Integer> getVisitedCustomers() {
            return visitedCustomers;
        }

        public void removeCustomers(Set<Integer> customers) {
            visitedCustomers.removeAll(customers);
        }
    }

}
