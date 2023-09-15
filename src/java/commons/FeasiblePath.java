package commons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeasiblePath {

    private final List<Integer> nodes;
    private final Set<Integer> customersServed;
    private final List<Integer> weights;

    public FeasiblePath(List<Integer> nodes, Set<Integer> customersServed, List<Integer> weights) {
        this.nodes = nodes;
        this.customersServed = customersServed;
        this.weights = weights;
    }

    public FeasiblePath() {
        this(new ArrayList<>(), new HashSet<>(), new ArrayList<>());
    }

    public static FeasiblePath getCopyWithoutCustomers(FeasiblePath p) {
        return new FeasiblePath(new ArrayList<>(p.nodes), new HashSet<>(), new ArrayList<>(p.weights));
    }

    public Set<Integer> getCustomersServed() {
        return customersServed;
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
        return "FeasiblePath{" + "nodes=" + nodes + ", customersServed=" + customersServed + ", weights=" + weights +
                '}';
    }

    public boolean isCustomerServed(int customer) {
        return customersServed.contains(customer);
    }

    public void removeCustomer(int customer) {
        customersServed.remove(customer);
    }

    public boolean containsNode(int node) {
        return nodes.contains(node);
    }
}
