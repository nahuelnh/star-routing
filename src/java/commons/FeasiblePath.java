package commons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    public List<Integer> getNodes() {
        return nodes;
    }

    public Set<Integer> getCustomersServed() {
        return customersServed;
    }

    public void addNode(int node, int weight) {
        nodes.add(node);
        weights.add(weight);
    }

    public void addCustomers(Collection<Integer> customers) {
        customersServed.addAll(customers);
    }

    public Integer getCost() {
        return weights.stream().reduce(Integer::sum).orElse(0);
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

    public FeasiblePath getCopyWithoutCustomers() {
        return new FeasiblePath(List.copyOf(this.nodes), new HashSet<>(), List.copyOf(this.weights));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeasiblePath that = (FeasiblePath) o;
        return Objects.equals(nodes, that.nodes) && Objects.equals(customersServed, that.customersServed) &&
                Objects.equals(weights, that.weights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, customersServed, weights);
    }
}
