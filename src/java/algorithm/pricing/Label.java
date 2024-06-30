package algorithm.pricing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public record Label(
    int demand, double cost, int node, BitSet visitedNodes, BitSet visitedCustomers, Label parent) {

  public static Label getRootLabel(int startNode, int numberOfNodes, double cost) {
    BitSet visitedNodes = new BitSet(numberOfNodes);
    BitSet visitedCustomers = new BitSet(numberOfNodes);
    visitedNodes.set(startNode);
    return new Label(0, cost, startNode, visitedNodes, visitedCustomers, null);
  }

  public boolean isNodeVisited(int node) {
    return visitedNodes.get(node);
  }

  public boolean isCustomerVisited(int customer) {
    return visitedCustomers.get(customer);
  }

  public boolean containsEdge(int i, int j) {
    List<Integer> nodes = getNodesInOrder();
    for (int index = 1; index < nodes.size(); index++) {
      int lastNode = nodes.get(index - 1);
      int currentNode = nodes.get(index);
      if (lastNode == i && currentNode == j) {
        return true;
      }
    }
    return false;
  }

  public boolean forbidsEdge(int i, int j) {
    List<Integer> nodes = getNodesInOrder();
    for (int index = 1; index < nodes.size(); index++) {
      int lastNode = nodes.get(index - 1);
      int currentNode = nodes.get(index);
      if (lastNode == i && currentNode != j) {
        return true;
      }
      if (lastNode != i && currentNode == j) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "Label{"
        + "demand="
        + demand
        + ", cost="
        + cost
        + ", node="
        + node
        + ", parent="
        + (parent == null ? null : parent.node)
        + '}';
  }

  public List<Integer> getNodesInOrder() {
    return getNodesInOrder(false);
  }

  public List<Integer> getNodesInOrder(boolean reverseOrder) {
    Label lastLabel = this;
    Label currentLabel = parent;
    List<Integer> nodes = new ArrayList<>();
    nodes.add(node);
    while (currentLabel != null) {
      if (lastLabel.node != currentLabel.node) {
        nodes.add(currentLabel.node);
      }
      lastLabel = currentLabel;
      currentLabel = currentLabel.parent;
    }
    if (!reverseOrder) {
      Collections.reverse(nodes);
    }
    return nodes;
  }

  //  public FeasiblePath translateToFeasiblePath(ESPPRCGraph graph) {
  //    FeasiblePath path = new FeasiblePath();
  //    List<Integer> nodes = getNodesInOrder();
  //    for (int j = 1; j < nodes.size(); j++) {
  //      int lastNode = nodes.get(j - 1);
  //      int currentNode = nodes.get(j);
  //      path.addNode(
  //          graph.translateFromESPPRCNode(currentNode),
  //          graph.getEdge(lastNode, currentNode).getWeight());
  //    }
  //    path.addCustomers(Utils.bitSetToIntSet(visitedCustomers));
  //    return path;
  //  }

}
