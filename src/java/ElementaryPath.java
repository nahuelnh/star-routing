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
    }

    public ElementaryPath() {
        this.nodes = new ArrayList<>();
        this.customersServed = new HashSet<>();
        this.weights = new ArrayList<>();
    }

    public void addNode(int node, int weight) {
        nodes.add(node);
        weights.add(weight);
    }

    public void addCustomers(Set<Integer> customers) {
        customersServed.addAll(customers);
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
