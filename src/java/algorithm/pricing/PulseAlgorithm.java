package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PulseAlgorithm {
    private static final double EPSILON = 1e-6;
    private static final int STEP = 1; // ~(Q / |S|)
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final int numberOfNodes;
    private final Map<Integer, Double> dualValues;
    private Stopwatch stopwatch;
    private double[][] lowerBounds;
    private double bestSolutionFound;
    private List<PartialPath> foundPartialPaths;
    private boolean saveSolution;
    private int pulsesPropagated;

    public PulseAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.graph = new ESPPRCGraph(instance);
        this.numberOfNodes = graph.getSize();
        this.dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        // Heuristic: sort decreasingly by benefit/cost
        this.graph.sortReverseNeighborhoods(
                Comparator.comparingDouble(s -> -dualValues.getOrDefault(s, 0.0) / instance.getDemand(s)));
        this.pulsesPropagated = 0;
    }

    private void resetGlobalOptimum() {
        bestSolutionFound = Double.MAX_VALUE;
        foundPartialPaths = new ArrayList<>();
        saveSolution = false;
    }

    public List<FeasiblePath> run(Duration timeLimit) {
        this.stopwatch = new Stopwatch(timeLimit);
        resetGlobalOptimum();
        bound();

        resetGlobalOptimum();
        saveSolution = true;
        pulseWithNodeRule(graph.getStart(), new PartialPath(-rmpSolution.getVehiclesDual(), 0));

        return translatePulsesToPaths();
    }

    private List<FeasiblePath> translatePulsesToPaths() {
        List<FeasiblePath> ret = new ArrayList<>();
        for (PartialPath currentPulse : foundPartialPaths) {
            FeasiblePath path = new FeasiblePath();
            int lastNode = currentPulse.getNodeAt(0);
            assert lastNode == instance.getDepot();
            assert lastNode == graph.getStart();
            for (int j = 1; j < currentPulse.getSize(); j++) {
                int currentNode = currentPulse.getNodeAt(j);
                if (currentNode == graph.getEnd()) {
                    currentNode = instance.getDepot();
                }
                path.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
                lastNode = currentNode;
            }
            path.addCustomers(Utils.bitSetToIntSet(currentPulse.getVisitedCustomers()));
            ret.add(path);
        }
        return ret;
    }

    private boolean pruneWithNodeRule(int nextNode, PartialPath visitedPath) {
        int totalDemand = visitedPath.getTotalDemand();
        double newEdgeCost = visitedPath.getSize() == 0 ? 0 : graph.getWeight(visitedPath.getLastNode(), nextNode);
        double totalCost = visitedPath.getTotalCost() + newEdgeCost;
        if (!isFeasible(nextNode, visitedPath)) {
            return true;
        }
        if (checkBounds(nextNode, totalCost, totalDemand)) {
            return true;
        }
        //return false;
        return rollback(nextNode, visitedPath);
    }

    private boolean pruneWithCustomerRule(int nextCustomer, int currentNode, PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand() + instance.getDemand(nextCustomer);
        double currentCost = visitedPath.getTotalCost() - dualValues.get(nextCustomer);
        if (visitedPath.isCustomerVisited(nextCustomer)) {
            return true;
        }
        if (currentDemand > instance.getCapacity()) {
            return true;
        }
        if (dualValues.get(nextCustomer) < EPSILON) {
            // Heuristic: if customer provides no reduction in total cost, can be pruned
            return true;
        }
        return checkBounds(currentNode, currentCost, currentDemand);
    }

    private void propagate(int currentNode, PartialPath visitedPath) {
        pulsesPropagated++;
        if (stopwatch.timedOut()) {
            return;
        }
        if (currentNode == graph.getEnd()) {
            if (visitedPath.getTotalCost() < bestSolutionFound) {
                bestSolutionFound = visitedPath.getTotalCost();
                if (saveSolution && bestSolutionFound < -EPSILON) {
                    foundPartialPaths.add(new PartialPath(visitedPath));
                }
            }
        } else {
            for (int nextCustomer : graph.getReverseNeighborhood(currentNode)) {
                pulseWithCustomerRule(currentNode, nextCustomer, visitedPath);
            }
            for (int nextNode : graph.getAdjacentNodes(currentNode)) {
                pulseWithNodeRule(nextNode, visitedPath);
            }
        }
    }

    private void pulseWithNodeRule(int currentNode, PartialPath visitedPath) {
        if (!pruneWithNodeRule(currentNode, visitedPath)) {
            visitedPath.addNode(currentNode);
            propagate(currentNode, visitedPath);
            visitedPath.removeLastNode();
        }
    }

    private void pulseWithCustomerRule(int currentNode, int currentCustomer, PartialPath visitedPath) {
        if (!pruneWithCustomerRule(currentCustomer, currentNode, visitedPath)) {
            visitedPath.addCustomer(currentCustomer);
            propagate(currentNode, visitedPath);
            visitedPath.removeCustomer(currentCustomer);
        }
    }

    private int bucketNumber(int q) {
        return q / STEP;
    }

    private void bound() {
        int N = graph.getSize();
        int Q = instance.getCapacity();

        lowerBounds = new double[N][bucketNumber(Q) + 1];
        for (int i = 0; i < N; i++) {
            Arrays.fill(lowerBounds[i], -Double.MAX_VALUE);
        }

        // for (int demand = 0; demand <= Q; demand += STEP) {
        for (int demand = Q - (Q % STEP); demand >= 0; demand -= STEP) {
            for (int node = 0; node < N; node++) {
                resetGlobalOptimum();
                PartialPath partialPath = new PartialPath(0.0, demand);
                pulseWithNodeRule(node, partialPath);
                lowerBounds[node][bucketNumber(demand)] = bestSolutionFound;
            }
        }
    }

    private boolean checkBounds(int currentNode, double cost, int demand) {
        // Returns true if the branch is to be pruned

        int bucket = bucketNumber(demand);

        if (lowerBounds[currentNode][bucket] == Double.MAX_VALUE) {
            // No feasible path to end node exists
            return true;
        }
        if (bestSolutionFound == Double.MAX_VALUE) {
            // Global solution not initialized, any solution will be better
            return false;
        }
        if (lowerBounds[currentNode][bucket] == -Double.MAX_VALUE) {
            // Matrix not initialized. Should only happen during bound phase
            return false;
        }
        return cost + lowerBounds[currentNode][bucket] >= bestSolutionFound;
    }

    private boolean isFeasible(int nextNode, PartialPath visitedPath) {
        // Adding node produces no cycles and demand does not exceed capacity
        return !visitedPath.isNodeVisited(nextNode) && visitedPath.getTotalDemand() <= instance.getCapacity();
    }

    private boolean rollback(int nextNode, PartialPath visitedPath) {
        // True iff node is to be pruned using rollback strategy
        int size = visitedPath.getSize();
        if (size <= 1) {
            return false;
        }
        int lastNode = visitedPath.getLastNode();
        if (!graph.edgeExists(lastNode, nextNode)) {
            return false;
        }
        double newEdgeCost = graph.getWeight(lastNode, nextNode);
        double newTotalCost = visitedPath.getPartialCostAt(size - 1) + newEdgeCost;
        for (int i = size - 2; i >= 0; i--) {
            int innerNode = visitedPath.getNodeAt(i);
            if (graph.edgeExists(innerNode, nextNode)) {
                double directEdgeCost = graph.getWeight(innerNode, nextNode);
                if (newTotalCost >= visitedPath.getPartialCostAt(i) + directEdgeCost) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getPulsesPropagated() {
        return pulsesPropagated;
    }

    private class PartialPath {
        private final int[] nodes;
        private final BitSet visitedCustomers;
        private final BitSet visitedNodes;
        private final double[] partialCosts;
        private int size;
        private double totalCost;
        private int totalDemand;

        public PartialPath(double totalCost, int totalDemand) {
            this.nodes = new int[numberOfNodes];
            this.size = 0;
            this.totalCost = totalCost;
            this.totalDemand = totalDemand;
            this.visitedCustomers = new BitSet(numberOfNodes);
            this.visitedNodes = new BitSet(numberOfNodes);
            this.partialCosts = new double[numberOfNodes];
            Arrays.fill(partialCosts, 0.0);
        }

        public PartialPath(PartialPath p) {
            this.nodes = Arrays.copyOf(p.nodes, numberOfNodes);
            this.visitedCustomers = (BitSet) p.visitedCustomers.clone();
            this.visitedNodes = (BitSet) p.visitedNodes.clone();
            this.partialCosts = Arrays.copyOf(p.partialCosts, numberOfNodes);
            this.size = p.size;
            this.totalCost = p.totalCost;
            this.totalDemand = p.totalDemand;
        }

        public BitSet getVisitedCustomers() {
            return visitedCustomers;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public int getTotalDemand() {
            return totalDemand;
        }

        public void addNode(int node) {
            nodes[size] = node;
            visitedNodes.set(node);
            totalCost += size == 0 ? 0.0 : graph.getWeight(nodes[size - 1], node);
            partialCosts[size] = totalCost;
            size++;
        }

        public void removeLastNode() {
            assert size >= 1;
            partialCosts[size - 1] = 0.0;
            visitedNodes.flip(nodes[size - 1]);
            totalCost -= size < 2 ? 0.0 : graph.getWeight(nodes[size - 2], nodes[size - 1]);
            nodes[size - 1] = -1;
            size--;
        }

        public void addCustomer(int customer) {
            visitedCustomers.set(customer);
            totalDemand += instance.getDemand(customer);
            totalCost -= dualValues.get(customer);
            partialCosts[size - 1] = totalCost;
        }

        public void removeCustomer(int customer) {
            visitedCustomers.flip(customer);
            totalDemand -= instance.getDemand(customer);
            totalCost += dualValues.get(customer);
            partialCosts[size - 1] = totalCost;
        }

        public boolean isCustomerVisited(int customer) {
            return visitedCustomers.get(customer);
        }

        public boolean isNodeVisited(int node) {
            return visitedNodes.get(node);
        }

        public int getNodeAt(int index) {
            return nodes[index];
        }

        public int getLastNode() {
            return nodes[size - 1];
        }

        public double getPartialCostAt(int index) {
            return partialCosts[index];
        }

        public int getSize() {
            return size;
        }

        @Override
        public String toString() {
            return "PartialPath{" + "nodes=" + Arrays.toString(nodes) + ", size=" + size + ", totalCost=" + totalCost +
                    ", totalDemand=" + totalDemand + '}';
        }
    }
}
