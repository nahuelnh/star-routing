package algorithm.pricing;

import commons.FeasiblePath;
import commons.Utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public record Label(ESPPRCGraph.ESPPRCNode node, double cost, int demand, BitSet visitedNodes, BitSet visitedCustomers, Label parent) {

    public static Label getRootLabel(ESPPRCGraph.ESPPRCNode startNode, int numberOfSRNodes, double cost) {
        BitSet visitedNodes = new BitSet(numberOfSRNodes);
        BitSet visitedCustomers = new BitSet(numberOfSRNodes);
        visitedNodes.set(startNode.getSrNodeId());
        return new Label(startNode, cost, 0, visitedNodes, visitedCustomers, null);
    }

    public boolean isNodeVisited(int node) {
        return visitedNodes.get(node);
    }

    public boolean isCustomerVisited(int customer) {
        return visitedCustomers.get(customer);
    }

    public BitSet copyOfVisitedNodes() {
        return visitedNodes.get(0, visitedNodes.length());
    }

    public BitSet copyOfVisitedCustomers() {
        return visitedCustomers.get(0, visitedCustomers.length());
    }

    public boolean containsEdge(int i, int j) {
        Label lastLabel = this;
        Label currentLabel = parent;
        while (currentLabel != null) {
            if (currentLabel.node == i && lastLabel.node == j) {
                return true;
            }
            lastLabel = currentLabel;
            currentLabel = currentLabel.parent;
        }
        return false;
    }

    public boolean forbidsEdge(int i, int j) {
        Label lastLabel = this;
        Label currentLabel = parent;
        while (currentLabel != null) {
            if (currentLabel.node == i && lastLabel.node != j) {
                return true;
            }
            if (currentLabel.node != i && lastLabel.node == j) {
                return true;
            }
            lastLabel = currentLabel;
            currentLabel = currentLabel.parent;
        }
        return false;
    }
    @Override
    public String toString() {
        return "Label{" + "demand=" + demand + ", cost=" + cost + ", node=" + node + ", parent=" +
                (parent == null ? null : parent.node) + '}';
    }

    private List<Integer> getNodesInOrder() {
        List<Integer> nodes = new ArrayList<>();
        nodes.add(node);
        Label lastLabel = this;
        Label currentLabel = parent;
        while (currentLabel != null) {
            if (lastLabel.node != currentLabel.node) {
                nodes.add(currentLabel.node);
            }
            lastLabel = currentLabel;
            currentLabel = currentLabel.parent;
        }
        Collections.reverse(nodes);
        return nodes;
    }

    public FeasiblePath translateToFeasiblePath(ESPPRCGraph graph) {
        FeasiblePath path = new FeasiblePath();
        List<Integer> nodes = getNodesInOrder();
        for (int j = 1; j < nodes.size(); j++) {
            int lastNode = nodes.get(j - 1);
            int currentNode = nodes.get(j);
            path.addNode(graph.translateFromESPPRCNode(currentNode), graph.getEdge(lastNode, currentNode).getWeight());
        }
        path.addCustomers(Utils.bitSetToIntSet(visitedCustomers));
        return path;

    }

    public static class Resources{
        private int cost;
        private BitSet nodes;
        private BitSet customers;
    }

}

