import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElementaryPath {

    private final List<Integer> nodes;
    private final Map<Integer, Set<Integer>> customersServedByNode;
    private final List<Integer> weights;

    public ElementaryPath(List<Integer> nodes, Map<Integer, Set<Integer>> customersServedByNode, List<Integer> weights) {
        this.nodes = nodes;
        this.customersServedByNode = customersServedByNode;
        this.weights = weights;
        checkRep();
    }

    public static ElementaryPath emptyPath() {
        return new ElementaryPath(new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    private void checkRep() {
        assert nodes.size() == weights.size();
        assert customersServedByNode.size() <= nodes.size();
        assert nodes.stream().noneMatch(node -> node == Integer.MAX_VALUE);
        assert new HashSet<>(nodes).containsAll(customersServedByNode.keySet());
        Set<Integer> alreadyServed = new HashSet<>();
        for (int node : customersServedByNode.keySet()) {
            assert Collections.disjoint(alreadyServed, customersServedByNode.get(node));
            alreadyServed.addAll(customersServedByNode.get(node));
        }
    }

    public void addNode(int node, Set<Integer> customersVisited, int weight) {
        nodes.add(node);
        if (!customersServedByNode.containsKey(node)) {
            customersServedByNode.put(node, new HashSet<>());
        }
        customersServedByNode.get(node).addAll(customersVisited);
        weights.add(weight);
        checkRep();
    }

    public Integer getCost() {
        return weights.stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isServedAtNode(int node, int customer) {
        return customersServedByNode.containsKey(node) && customersServedByNode.get(node).contains(customer);
    }

    @Override
    public String toString() {
        return "Route{" + "nodes=" + nodes + ", customersServed=" + customersServedByNode + ", weights=" + weights + '}';
    }
}
