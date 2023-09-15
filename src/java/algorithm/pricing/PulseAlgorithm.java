package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PulseAlgorithm {
    private static final double EPSILON = 1e-6;
    private final Instance instance;
    private final RestrictedMasterProblem.RMPSolution rmpSolution;
    private final ESPPRCGraph graph;
    private final int numberOfNodes;
    private final Map<Integer, Double> dualValues;
    private double[][] lowerBounds;
    private double bestSolutionFound;
    private List<PartialPath> foundPartialPaths;
    private boolean saveSolution;

    public PulseAlgorithm(Instance instance, RestrictedMasterProblem.RMPSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.graph = new ESPPRCGraph(instance);
        this.numberOfNodes = graph.getSize();
        this.dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
    }

    private void resetGlobalOptimum() {
        bestSolutionFound = Double.MAX_VALUE;
        foundPartialPaths = new ArrayList<>();
        saveSolution = false;
    }

    public List<FeasiblePath> run() {
        resetGlobalOptimum();
        bound();

        resetGlobalOptimum();
        saveSolution = true;
        pulseWithNodeRule(graph.getStart(), new PartialPath());

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
            path.addCustomers(currentPulse.getVisitedCustomersAsSet());
            ret.add(path);
        }
        return ret;
    }

    private boolean pruneWithNodeRule(int currentNode, PartialPath visitedPath) {
        int totalDemand = visitedPath.getTotalDemand();
        double newEdgeCost = visitedPath.getSize() == 0 ? 0 : graph.getWeight(visitedPath.getLastNode(), currentNode);
        double totalCost = visitedPath.getTotalCost() + newEdgeCost;
        if (!isFeasible(currentNode, visitedPath)) {
            return true;
        }
        if (checkBounds(currentNode, totalCost, totalDemand)) {
            return true;
        }
        return rollback(currentNode, visitedPath);
    }

    private boolean isNegativeReducedCost(double cost) {
        return cost - rmpSolution.getVehiclesDual() < -EPSILON;
    }

    private boolean pruneWithCustomerRule(int currentCustomer, int currentNode, PartialPath visitedPath) {
        int currentDemand = visitedPath.getTotalDemand() + instance.getDemand(currentCustomer);
        double currentCost = visitedPath.getTotalCost() - dualValues.get(currentCustomer);
        //        if (dualValues.get(currentCustomer) < EPSILON) {
        //            return true;
        //        }
        if (currentDemand > instance.getCapacity()) {
            return true;
        }
        return checkBounds(currentNode, currentCost, currentDemand);
    }

    private void propagate(int currentNode, PartialPath visitedPath) {
        if (currentNode == graph.getEnd()) {
            if (visitedPath.getTotalCost() < bestSolutionFound) {
                bestSolutionFound = visitedPath.getTotalCost();
                if (saveSolution && isNegativeReducedCost(visitedPath.getTotalCost())) {
                    foundPartialPaths.add(new PartialPath(visitedPath));
                }
            }
        } else {
            for (int nextCustomer : graph.getReverseNeighborhood(currentNode)) {
                if (!visitedPath.isCustomerVisited(nextCustomer)) {
                    pulseWithCustomerRule(currentNode, nextCustomer, visitedPath);
                }
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

    private void bound() {
        lowerBounds = new double[numberOfNodes][instance.getCapacity() + 1];
        for (int i = 0; i < numberOfNodes; i++) {
            Arrays.fill(lowerBounds[i], -Double.MAX_VALUE);
        }
        //for (int capacity = 0; capacity <= instance.getCapacity(); capacity++) {
        for (int capacity = instance.getCapacity(); capacity >= 0; capacity--) {
            for (int node = 0; node < numberOfNodes; node++) {
                resetGlobalOptimum();
                PartialPath partialPath = new PartialPath(-rmpSolution.getVehiclesDual(), capacity);
                pulseWithNodeRule(node, partialPath);
                lowerBounds[node][capacity] = bestSolutionFound;
            }
        }
    }

    private boolean checkBounds(int currentNode, double cost, int demand) {
        // Returns true if the branch is to be pruned

        if (lowerBounds[currentNode][demand] == Double.MAX_VALUE) {
            // No feasible path to end node exists
            return true;
        }
        if (bestSolutionFound == Double.MAX_VALUE) {
            // Global solution not initialized, any solution will be better
            return false;
        }
        if (lowerBounds[currentNode][demand] == -Double.MAX_VALUE) {
            // Should never happen
            return false;
        }
        return cost + lowerBounds[currentNode][demand] >= bestSolutionFound;
    }

    private boolean isFeasible(int currentNode, PartialPath partialPath) {
        // Adding node produces no cycles and demand does not exceed capacity
        return !partialPath.isNodeVisited(currentNode) && partialPath.getTotalDemand() <= instance.getCapacity();
    }

    private boolean rollback(int currentNode, PartialPath visitedPath) {
        // True iff node is to be pruned using rollback strategy

        int size = visitedPath.getSize();
        if (size <= 1) {
            return false;
        }
        double directEdgeCost = graph.getWeight(visitedPath.getNodeAt(size - 2), currentNode);
        double newEdgeCost = graph.getWeight(visitedPath.getLastNode(), currentNode);
        return visitedPath.getPartialCostAt(size - 1) + newEdgeCost >=
                visitedPath.getPartialCostAt(size - 2) + directEdgeCost;

    }

    private class PartialPath {
        private final int[] nodes;
        private final boolean[] visitedCustomers;
        private final boolean[] visitedNodes;
        private final double[] partialCosts;
        private int size;
        private double totalCost;
        private int totalDemand;

        public PartialPath() {
            this(-rmpSolution.getVehiclesDual(), 0);
        }

        public PartialPath(double totalCost, int totalDemand) {
            this.nodes = new int[numberOfNodes];
            this.size = 0;
            this.totalCost = totalCost;
            this.totalDemand = totalDemand;
            this.visitedCustomers = new boolean[numberOfNodes];
            Arrays.fill(visitedCustomers, false);
            this.visitedNodes = new boolean[numberOfNodes];
            Arrays.fill(visitedNodes, false);
            this.partialCosts = new double[numberOfNodes];
            Arrays.fill(partialCosts, 0.0);
        }

        public PartialPath(PartialPath p) {
            this.nodes = Arrays.copyOf(p.nodes, numberOfNodes);
            this.visitedCustomers = Arrays.copyOf(p.visitedCustomers, numberOfNodes);
            this.visitedNodes = Arrays.copyOf(p.visitedNodes, numberOfNodes);
            this.partialCosts = Arrays.copyOf(p.partialCosts, numberOfNodes);
            this.size = p.size;
            this.totalCost = p.totalCost;
            this.totalDemand = p.totalDemand;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public int getTotalDemand() {
            return totalDemand;
        }

        public void addNode(int node) {
            nodes[size] = node;
            visitedNodes[node] = true;
            totalCost += size == 0 ? 0.0 : graph.getWeight(nodes[size - 1], node);
            partialCosts[size] = totalCost;
            size++;
        }

        public void removeLastNode() {
            assert size >= 1;
            partialCosts[size - 1] = 0.0;
            visitedNodes[nodes[size - 1]] = false;
            totalCost -= size <= 2 ? 0.0 : graph.getWeight(nodes[size - 2], nodes[size - 1]);
            size--;
        }

        public void addCustomer(int customer) {
            visitedCustomers[customer] = true;
            totalDemand += instance.getDemand(customer);
            totalCost -= dualValues.get(customer);
            partialCosts[size - 1] = totalCost;
        }

        public void removeCustomer(int customer) {
            visitedCustomers[customer] = false;
            totalDemand -= instance.getDemand(customer);
            totalCost += dualValues.get(customer);
            partialCosts[size - 1] = totalCost;
        }

        public boolean isCustomerVisited(int customer) {
            return visitedCustomers[customer];
        }

        public boolean isNodeVisited(int node) {
            return visitedNodes[node];
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

        public Set<Integer> getVisitedCustomersAsSet() {
            Set<Integer> ret = new HashSet<>();
            for (int i = 0; i < numberOfNodes; i++) {
                if (visitedCustomers[i]) {
                    ret.add(i);
                }
            }
            return ret;
        }
    }
}
