import java.util.List;
import java.util.Map;
import java.util.Set;

public class Route {

    private final List<Integer> nodes;
    private final Map<Integer, Set<Integer>> customersServed;
    private final List<Integer> weights;

    Route(List<Integer> nodes, Map<Integer, Set<Integer>> customersServed, List<Integer> weights) {
        this.nodes = nodes;
        this.customersServed = customersServed;
        this.weights = weights;
    }

    public Integer getCost() {
        return weights.stream().mapToInt(Integer::intValue).sum();
    }

    public boolean inRoute(int node, int customer) {
        return customersServed.containsKey(node) && customersServed.get(node).contains(customer);
    }

}
