package algorithm;

import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    public FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }

    private FeasiblePath createOneRedundantPath() {
        int minCost = Integer.MAX_VALUE;
        int bestNode = instance.getDepot() + 1;
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (i != instance.getDepot()) {
                int roundTripCost =
                        instance.getEdgeWeight(instance.getDepot(), i) + instance.getEdgeWeight(i, instance.getDepot());
                if (roundTripCost < minCost) {
                    minCost = roundTripCost;
                    bestNode = i;
                }
            }
        }
        FeasiblePath path = new FeasiblePath();
        path.addNode(bestNode, instance.getEdgeWeight(instance.getDepot(), bestNode));
        path.addNode(instance.getDepot(), instance.getEdgeWeight(bestNode, instance.getDepot()));
        return path;
    }

    public List<FeasiblePath> run() {
        int depot = instance.getDepot();
        List<FeasiblePath> ret = new ArrayList<>();

        FeasiblePath currentPath = new FeasiblePath();
        int cumulativeDemand = 0;
        int lastNode = depot;
        Set<Integer> visitedCustomers = new HashSet<>();
        for (int currentNode : instance.getCustomers()) {
            cumulativeDemand += instance.getDemand(currentNode);
            if (cumulativeDemand > instance.getCapacity()) {
                currentPath.addNode(depot, instance.getEdgeWeight(lastNode, depot));
                ret.add(currentPath);
                currentPath = new FeasiblePath();
                cumulativeDemand = 0;
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
        if (!instance.unusedVehiclesAllowed()) {
            // ret should have length equal to the number of vehicles
            FeasiblePath redundantPath = createOneRedundantPath();
            while (ret.size() < instance.getNumberOfVehicles()) {
                ret.add(redundantPath);
            }
        }
        return ret;
    }
}
