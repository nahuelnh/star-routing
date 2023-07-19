import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    public FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }

    private void addRedundantRoutes(List<ElementaryPath> ret) {
        int minCost = Integer.MAX_VALUE;
        int bestNode = 1;
        for (int i = 1; i < instance.getNumberOfNodes(); i++) {
            int cost = instance.getEdgeWeight(instance.getDepot(), i) + instance.getEdgeWeight(i, instance.getDepot());
            if (cost < minCost) {
                minCost = cost;
                bestNode = i;
            }
        }
        while (ret.size() < instance.getNumberOfVehicles()) {
            ElementaryPath path = ElementaryPath.emptyPath();
            path.addNode(bestNode, new HashSet<>(), instance.getEdgeWeight(instance.getDepot(), bestNode));
            path.addNode(instance.getDepot(), new HashSet<>(), instance.getEdgeWeight(bestNode, instance.getDepot()));
            ret.add(path);
        }
    }

    public List<ElementaryPath> run() {
        List<ElementaryPath> ret = new ArrayList<>();
        ElementaryPath currentElementaryPath = ElementaryPath.emptyPath();
        int cumulativeDemand = 0;
        int lastNode = instance.getDepot();
        for (int currentNode : instance.getCustomers()) {
            cumulativeDemand += instance.getDemand(currentNode);
            if (cumulativeDemand > instance.getCapacity()) {
                currentElementaryPath.addNode(instance.getDepot(), new HashSet<>(),
                        instance.getEdgeWeight(lastNode, instance.getDepot()));
                ret.add(currentElementaryPath);
                currentElementaryPath = ElementaryPath.emptyPath();
                cumulativeDemand = 0;
                lastNode = instance.getDepot();
            }
            currentElementaryPath.addNode(currentNode, Set.of(currentNode),
                    instance.getEdgeWeight(lastNode, currentNode));
            lastNode = currentNode;
        }
        currentElementaryPath.addNode(instance.getDepot(), new HashSet<>(),
                instance.getEdgeWeight(lastNode, instance.getDepot()));
        ret.add(currentElementaryPath);
        if (!instance.unusedVehiclesAllowed()) {
            // ret should have length equal to the number of vehicles
            addRedundantRoutes(ret);
        }
        return ret;
    }
}
