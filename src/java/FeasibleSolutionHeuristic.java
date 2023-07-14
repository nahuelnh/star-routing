import java.util.*;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }


    List<Route> run() {
        List<Route> ret = new ArrayList<>();
        List<Integer> nodes = new ArrayList<>();
        Map<Integer, Set<Integer>> customersServed = new HashMap<>();
        List<Integer> weights = new ArrayList<>();

        int cumulativeDemand = 0;
        nodes.add(0);
        customersServed.put(0, new HashSet<>());
        customersServed.get(0).add(0);
        int lastNode = 0;

        for (int currentNode = 1; currentNode < instance.getNodes(); currentNode++) {
            cumulativeDemand += instance.getDemand(currentNode);
            if (cumulativeDemand > instance.getCapacity()) {
                weights.add(instance.getGraphWeights(lastNode, instance.getDepot()));
                ret.add(new Route(nodes, customersServed, weights));
                nodes = new ArrayList<>();
                customersServed = new HashMap<>();
                weights = new ArrayList<>();
                cumulativeDemand = 0;
            }
            nodes.add(currentNode);
            customersServed.put(currentNode, new HashSet<>());
            customersServed.get(currentNode).add(currentNode);
            weights.add(instance.getGraphWeights(lastNode, currentNode));
            lastNode = currentNode;
        }
        return ret;
    }

}
