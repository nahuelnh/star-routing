import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    public FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }


    public List<Route> run() {
        List<Route> ret = new ArrayList<>();

        Route currentRoute = Route.emptyRoute();
        int cumulativeDemand = 0;
        int lastNode = instance.getDepot();

        for (int currentNode : instance.getCustomers()) {
            cumulativeDemand += instance.getDemand(currentNode);
            if (cumulativeDemand > instance.getCapacity()) {
                currentRoute.addNode(instance.getDepot(), new HashSet<>(), instance.getGraphWeights(lastNode, instance.getDepot()));
                ret.add(currentRoute);
                currentRoute = Route.emptyRoute();
                cumulativeDemand = 0;
                lastNode = instance.getDepot();
            }
            Set<Integer> customersServed = Stream.of(currentNode).collect(Collectors.toSet());
            currentRoute.addNode(currentNode, customersServed, instance.getGraphWeights(lastNode, currentNode));
            lastNode = currentNode;
        }
        currentRoute.addNode(instance.getDepot(), new HashSet<>(), instance.getGraphWeights(lastNode, instance.getDepot()));
        ret.add(currentRoute);
        return ret;
    }
}
