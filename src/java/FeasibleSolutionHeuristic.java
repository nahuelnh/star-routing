import java.util.*;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }


    List<Route> run() {
        int cumulativeDemand = 0;
        List<Route> ret = new ArrayList<>();
        for (int currentNode = 0; currentNode < instance.getNodes(); currentNode++) {
            List<Integer> nodes = new ArrayList<>();
            Map<Integer, Set<Integer>> customersServed = new HashMap<>();
            cumulativeDemand += instance.getDemand(currentNode);

            if (cumulativeDemand > instance.getCapacity()) {
                ret.add(new Route(nodes, customersServed));
                nodes = new ArrayList<>();
                customersServed = new HashMap<>();
            }

            nodes.add(currentNode);
            customersServed.put(currentNode, new HashSet<>());
            customersServed.get(currentNode).add(currentNode);
        }

        return ret;
    }

}
