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
    private static final double EPSILON = 1e-6;

    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final double[][] lowerBounds;
    private final int endNode;
    private final int startNode;
    private final int numberOfNodes;
    private final List<Node> graph;
    private double bestSolutionFound;
    private List<Node> bestPath;
    private Set<Integer> bestCustomers;

    public PulseAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            System.out.println("--- " + s + " " + rmpSolution.getCustomerDual(s));
        }
        System.out.println("+++ " + rmpSolution.getVehiclesDual());
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
        bestCustomers = new HashSet<>();
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

    private List<FeasiblePath> computePathsFromSolution() {
        if (bestSolutionFound - rmpSolution.getVehiclesDual() < -EPSILON) {
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
            path.addCustomers(bestCustomers);

            System.out.println("bestFound " + bestSolutionFound + " " + path.getCost());
            return List.of(path);
        }
        return new ArrayList<>();

    }

    public List<FeasiblePath> getOptimalPaths() {
        bound();
        bestSolutionFound = Double.MAX_VALUE;
        pulse(graph.get(startNode), 0.0, new PartialPath());
        //        System.out.println(bestSolutionFound);
        //        System.out.println(bestPath);
        //        System.out.println(bestCustomers);
        //        System.out.println(computePathsFromSolution());
        return computePathsFromSolution();
    }

    private boolean prune(Node currentNode, double edgeCost, PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand();
        double currentCost = visitedPath.getTotalCost() + edgeCost;
        if (!isFeasible(currentNode, currentDemand)) {
            return true;
        }
        if (checkBounds(currentNode, currentCost, currentDemand)) {
            return true;
        }
        if (rollback(currentNode, visitedPath)) {
            return true;
        }
        return false;
    }

    private boolean prunePulseCustomer(Node currentNode, int deltaDemand, double deltaCost, PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand() + deltaDemand;
        double currentCost = visitedPath.getTotalCost() - deltaCost;

        if (deltaCost < EPSILON && deltaCost > -EPSILON) {
            return true;
        }
        if (currentDemand > instance.getCapacity()) {
            return true;
        }
        if (checkBounds(currentNode, currentCost, currentDemand)) {
            return true;
        }
        if (rollback(currentNode, visitedPath)) {
            return true;
        }
        return false;
    }

    private void propagate(Node currentNode, PartialPath visitedPath) {
        //        System.out.println(
        //                currentNode.getNumber() + " " + visitedPath.nodes.stream().map(Node::getNumber).toList() + " " +
        //                        visitedPath.visitedCustomers + " " + bestSolutionFound);

        if (currentNode.getNumber() == endNode) {

            if (visitedPath.getTotalCost() < bestSolutionFound) {
                bestSolutionFound = visitedPath.getTotalCost();
                bestPath = visitedPath.getCopyOfNodes();
                bestCustomers = visitedPath.getCopyOfCustomers();
            }
        } else {

            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node nextNode = graph.get(edge.getHead());
                pulse(nextNode, edge.getCost(), visitedPath);
            }
            for (int nextCustomer : currentNode.getReverseNeighborhood()) {
                if (!visitedPath.isVisited(nextCustomer)) {
                    pulseCustomer(currentNode, nextCustomer, visitedPath);
                }
            }
        }
    }

    private void pulse(Node currentNode, double edgeCost, PartialPath visitedPath) {
        if (!prune(currentNode, edgeCost, visitedPath)) {
            currentNode.visit();
            visitedPath.addNode(currentNode, edgeCost);
            propagate(currentNode, visitedPath);
            currentNode.unvisit();
            visitedPath.removeLast(edgeCost);
        }
    }

    private void pulseCustomer(Node currentNode, int customer, PartialPath visitedPath) {
        int deltaDemand = instance.getDemand(customer);
        double deltaCost = rmpSolution.getCustomerDual(instance.getCustomerIndex(customer));
        if (!prunePulseCustomer(currentNode, deltaDemand, deltaCost, visitedPath)) {
            visitedPath.addCustomer(customer, deltaDemand, deltaCost);
            propagate(currentNode, visitedPath);
            visitedPath.removeCustomer(customer, deltaDemand, deltaCost);
        }
    }

    private void bound() {
        for (int capacity = instance.getCapacity(); capacity >= 0; capacity--) {
            for (Node node : graph) {
                System.out.println(node.getNumber() + " " + capacity);
                bestSolutionFound = Double.MAX_VALUE;
                PartialPath partialPath = new PartialPath(0.0, capacity);
                pulse(node, 0.0, partialPath);
                lowerBounds[node.getNumber()][capacity] = bestSolutionFound;
            }
        }
        assert false;
    }

    private boolean checkBounds(Node currentNode, double cost, int demand) {
        // Returns true if the branch is to be pruned
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

    private boolean isFeasible(Node currentNode, int demand) {
        return !currentNode.isVisited() && demand <= instance.getCapacity();
    }

    private boolean rollback(Node currentNode, PartialPath visitedPath) {
        return false;
    }

    private class Node {
        private final int number;

        private final List<Edge> outgoingEdges;

        private final Set<Integer> reverseNeighborhood;

        private boolean visited;

        public Node(int number) {
            this.number = number;
            this.outgoingEdges = new ArrayList<>();
            this.reverseNeighborhood = computeReverseNeighborhood();
            this.visited = false;
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
            this.totalCost = 0.0;
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

        public void addNode(Node node, double edgeCost) {
            nodes.addLast(node);
            totalCost += edgeCost;
        }

        public void removeLast(double edgeCost) {
            nodes.removeLast();
            totalCost -= edgeCost;
        }

        public List<Node> getCopyOfNodes() {
            return new ArrayList<>(nodes);
        }

        public Set<Integer> getCopyOfCustomers() {
            return new HashSet<>(visitedCustomers);
        }

        public void addCustomer(int customer, int deltaDemand, double deltaCost) {
            visitedCustomers.add(customer);
            totalDemand += deltaDemand;
            totalCost -= deltaCost;
        }

        public void removeCustomer(int customer, int deltaDemand, double deltaCost) {
            visitedCustomers.remove(customer);
            totalDemand -= deltaDemand;
            totalCost += deltaCost;
        }

        public boolean isVisited(int customer) {
            return visitedCustomers.contains(customer);
        }
    }

}
