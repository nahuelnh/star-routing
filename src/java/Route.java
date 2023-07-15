import java.util.*;

public class Route {

    private final List<Integer> nodes;
    private final Map<Integer, Set<Integer>> customersServed;
    private final List<Integer> weights;

    public Route(List<Integer> nodes, Map<Integer, Set<Integer>> customersServed, List<Integer> weights) {
        this.nodes = nodes;
        this.customersServed = customersServed;
        this.weights = weights;
    }

    public static Route emptyRoute() {
        return new Route(new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    public void addNode(int node, Set<Integer> customersVisited, int weight) {
        nodes.add(node);
        if (!customersServed.containsKey(node)) {
            customersServed.put(node, new HashSet<>());
        }
        customersServed.get(node).addAll(customersVisited);
        weights.add(weight);
    }

    public Integer getCost() {
        return weights.stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isServedAtNode(int node, int customer) {
        return customersServed.containsKey(node) && customersServed.get(node).contains(customer);
    }

    @Override
    public String toString() {
        return "Route{" +
                "nodes=" + nodes +
                ", customersServed=" + customersServed +
                ", weights=" + weights +
                '}';
    }
}
