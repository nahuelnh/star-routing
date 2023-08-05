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
        this.nodes = new ArrayList<>();
        this.customersServed = new HashSet<>();
        this.weights = new ArrayList<>();
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
}
