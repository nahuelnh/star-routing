import java.util.List;
import java.util.Map;
import java.util.Set;

public class Route {

    private final List<Integer> nodes;
    private final Map<Integer, Set<Integer>> customersServed;

    Route(List<Integer> nodes, Map<Integer, Set<Integer>> customersServed) {
        this.nodes = nodes;
        this.customersServed = customersServed;
    }

    public List<Integer> getNodes() {
        return nodes;
    }

    public boolean inRoute(int node, int customer) {
        return customersServed.containsKey(node) && customersServed.get(node).contains(customer);
    }

}
