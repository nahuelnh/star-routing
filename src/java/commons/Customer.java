package commons;

import java.util.HashSet;
import java.util.Set;

public class Customer {

    private final int index;
    private final int id;
    private final int demand;
    private final Set<Integer> neighbors;

    public Customer(int index, int id, int demand) {
        this.index = index;
        this.id = id;
        this.demand = demand;
        this.neighbors = new HashSet<>();
        this.neighbors.add(id);
    }

    public int getIndex() {
        return index;
    }

    public int getId() {
        return id;
    }

    public void addNeighbor(int node) {
        neighbors.add(node);
    }

    public Set<Integer> getNeighbors() {
        return neighbors;
    }

    public int getDemand() {
        return demand;
    }
}
