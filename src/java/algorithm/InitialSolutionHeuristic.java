package algorithm;

import commons.Route;
import commons.Instance;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InitialSolutionHeuristic {

  private final Instance instance;

  public InitialSolutionHeuristic(Instance instance) {
    this.instance = instance;
  }

  private int getTotalDemand(Route path) {
    return path.getCustomersServed().stream()
        .map(instance::getDemand)
        .reduce(Integer::sum)
        .orElse(0);
  }

  private Optional<Route> computeReplacement(Route path1, Route path2) {
    if (getTotalDemand(path1) + getTotalDemand(path2) <= instance.getCapacity()) {
      Route ret = new Route();
      BitSet visited = new BitSet(instance.getNumberOfNodes());
      int lastNode = instance.getDepot();
      for (int node : path1.getNodes()) {
        if (node != instance.getDepot()) {
          ret.addNode(node, instance.getEdgeWeight(lastNode, node));
          visited.set(node);
          lastNode = node;
        }
      }
      for (int node : path2.getNodes()) {
        if (!visited.get(node)) {
          ret.addNode(node, instance.getEdgeWeight(lastNode, node));
          visited.set(node);
          lastNode = node;
        }
      }
      ret.addCustomers(path1.getCustomersServed());
      ret.addCustomers(path2.getCustomersServed());
      return Optional.of(ret);
    }
    return Optional.empty();
  }

  private List<Route> mergeHeuristic(List<Route> pathsToBeMerged) {
    BitSet merged = new BitSet(pathsToBeMerged.size());
    for (int i = 0; i < pathsToBeMerged.size(); i++) {
      Route path1 = pathsToBeMerged.get(i);
      for (int j = 0; j < i; j++) {
        Route path2 = pathsToBeMerged.get(j);
        if (!merged.get(i) && !merged.get(j)) {
          Optional<Route> replacement = computeReplacement(path1, path2);
          if (replacement.isPresent()) {
            pathsToBeMerged.add(replacement.get());
            merged.set(i);
            merged.set(j);
          }
        }
      }
    }
    List<Route> ret = new ArrayList<>();
    for (int i = 0; i < pathsToBeMerged.size(); i++) {
      if (!merged.get(i)) {
        ret.add(pathsToBeMerged.get(i));
      }
    }
    return ret;
  }

  private Route createOneRedundantPath() {
    int minCost = Integer.MAX_VALUE;
    int bestNode = instance.getDepot() + 1;
    for (int i = 0; i < instance.getNumberOfNodes(); i++) {
      if (i != instance.getDepot()) {
        int roundTripCost =
            instance.getEdgeWeight(instance.getDepot(), i)
                + instance.getEdgeWeight(i, instance.getDepot());
        if (roundTripCost < minCost) {
          minCost = roundTripCost;
          bestNode = i;
        }
      }
    }
    Route path = new Route();
    path.addNode(bestNode, instance.getEdgeWeight(instance.getDepot(), bestNode));
    path.addNode(instance.getDepot(), instance.getEdgeWeight(bestNode, instance.getDepot()));
    return path;
  }

  public List<Route> run() {
    int depot = instance.getDepot();
    List<Route> ret = new ArrayList<>();
    Route currentPath = new Route();
    int cumulativeDemand = 0;
    int lastNode = depot;
    Set<Integer> visitedCustomers = new HashSet<>();
    for (int currentNode : instance.getCustomers()) {
      cumulativeDemand += instance.getDemand(currentNode);
      if (cumulativeDemand > instance.getCapacity()) {
        currentPath.addNode(depot, instance.getEdgeWeight(lastNode, depot));
        ret.add(currentPath);
        currentPath = new Route();
        cumulativeDemand = instance.getDemand(currentNode);
        lastNode = depot;
        visitedCustomers.clear();
      }
      visitedCustomers.add(currentNode);
      currentPath.addNode(currentNode, instance.getEdgeWeight(lastNode, currentNode));
      currentPath.addCustomers(visitedCustomers);
      lastNode = currentNode;
    }
    currentPath.addNode(depot, instance.getEdgeWeight(lastNode, depot));
    ret.add(currentPath);
    if (ret.size() > instance.getNumberOfVehicles()) {
      ret = mergeHeuristic(ret);
    }
    if (!instance.unusedVehiclesAllowed()) {
      // ret should have length equal to the number of vehicles
      Route redundantPath = createOneRedundantPath();
      while (ret.size() < instance.getNumberOfVehicles()) {
        ret.add(redundantPath);
      }
    }
    return ret;
  }
}
