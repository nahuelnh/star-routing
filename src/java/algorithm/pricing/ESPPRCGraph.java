package algorithm.pricing;

import commons.Instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ESPPRCGraph {

    private final Instance instance;
    private final int size;
    private final int start;
    private final int end;
    private final List<List<Integer>> adjacency;
    private final int[][] weights;
    private final List<Set<Integer>> reverseNeighborhoods;

    public ESPPRCGraph(Instance instance) {
        this.instance = instance;
        this.size = instance.getNumberOfNodes() + 1;
        this.start = instance.getDepot();
        this.end = size - 1;
        this.adjacency = new ArrayList<>(size);
        this.weights = new int[size][size];
        this.reverseNeighborhoods = new ArrayList<>(size);
        createGraph();
    }

    private void createGraph() {
        for (int i = 0; i < size; i++) {
            adjacency.add(new ArrayList<>(size));
            for (int j = 0; j < size; j++) {
                weights[i][j] = -1;
            }
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j && i != end && j != start && !(i == start && j == end)) {
                    adjacency.get(i).add(j);
                    if (j == end) {
                        weights[i][j] = instance.getEdgeWeight(i, instance.getDepot());
                    } else {
                        weights[i][j] = instance.getEdgeWeight(i, j);
                    }
                    reverseNeighborhoods.add(computeReverseNeighborhood(i));
                }
            }
        }
    }

    private Set<Integer> computeReverseNeighborhood(int node) {
        if (node == end) {
            node = instance.getDepot();
        }
        Set<Integer> ret = new HashSet<>();
        for (int customer : instance.getCustomers()) {
            if (instance.getNeighbors(customer).contains(node)) {
                ret.add(customer);
            }
        }
        return ret;
    }

    public int getSize() {
        return size;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public List<Integer> getAdjacentNodes(int node) {
        return adjacency.get(node);
    }

    public int getWeight(int i, int j) {
        return weights[i][j];
    }

    public Set<Integer> getReverseNeighborhood(int node) {
        return reverseNeighborhoods.get(node);
    }
}
