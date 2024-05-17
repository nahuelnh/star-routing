package algorithm.pricing;

import commons.Graph;
import commons.Instance;

import java.util.List;

public class ESPPRCGraph extends Graph {

    private final Instance instance;
    private final int start;
    private final int end;

    public ESPPRCGraph(Instance instance) {
        super(instance.getNumberOfNodes() + 1);
        this.instance = instance;
        this.start = instance.getDepot();
        this.end = instance.getNumberOfNodes();
        createGraph();
    }

    private void createGraph() {
        for (int i = 0; i < getSize(); i++) {
            for (int j = 0; j < getSize(); j++) {
                if (i != j && i != end && j != start && !(i == start && j == end)) {
                    if (j == end) {
                        addEdge(i, j, instance.getEdgeWeight(i, instance.getDepot()));
                    } else {
                        addEdge(i, j, instance.getEdgeWeight(i, j));
                    }
                }
            }
        }
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int translateToESPPRCNode(int node) {
        return node == instance.getDepot() ? end : node;
    }

    public List<Integer> getReverseNeighborhood(int node) {
        return instance.getReverseNeighborhood(node == end ? instance.getDepot() : node);
    }
}
