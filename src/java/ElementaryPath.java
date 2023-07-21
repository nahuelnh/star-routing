import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElementaryPath {

    private final List<Integer> nodes;
    private final Set<Integer> customersServed;
    private final List<Integer> weights;

    public ElementaryPath(List<Integer> nodes, Set<Integer> customersServed, List<Integer> weights) {
        this.nodes = nodes;
        this.customersServed = customersServed;
        this.weights = weights;
        checkRep();
    }

    public static ElementaryPath emptyPath() {
        return new ElementaryPath(new ArrayList<>(), new HashSet<>(), new ArrayList<>());
    }

    private void checkRep() {
        assert nodes.size() == weights.size();
    }

    public void addNode(int node, Set<Integer> customersVisited, int weight) {
        nodes.add(node);
        customersServed.addAll(customersVisited);
        weights.add(weight);
        checkRep();
    }

    public Integer getCost() {
        return weights.stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        return "ElementaryPath{" + "nodes=" + nodes + ", customersServed=" + customersServed + ", weights=" + weights +
                '}';
    }

    public boolean isCustomerServed(int customer) {
        return customersServed.contains(customer);
    }
}
