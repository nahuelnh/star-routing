package algorithm.pricing;

import commons.Graph;
import commons.Instance;

import java.util.List;

public class ESPPRCGraph extends Graph {

  private final Instance instance;
  private final int source;
  private final int sink;

  public ESPPRCGraph(Instance instance) {
    this(instance, false);
  }

  public ESPPRCGraph(Instance instance, boolean reversed) {
    super(instance.getNumberOfNodes() + 1);
    this.instance = instance;
    this.source = instance.getDepot();
    this.sink = instance.getNumberOfNodes();
    if (reversed) {
      createGraph(sink, source);
    } else {
      createGraph(source, sink);
    }
  }

  private void createGraph(int start, int end) {
    for (int i = 0; i < getSize(); i++) {
      for (int j = 0; j < getSize(); j++) {
        if (i != j && i != end && j != start && !(i == start && j == end)) {
          if (j == sink) {
            addEdge(i, j, instance.getEdgeWeight(i, instance.getDepot()));
          } else if (i == sink) {
            addEdge(i, j, instance.getEdgeWeight(instance.getDepot(), j));
          } else {
            addEdge(i, j, instance.getEdgeWeight(i, j));
          }
        }
      }
    }
  }

  public int getSource() {
    return source;
  }

  public int getSink() {
    return sink;
  }

  public int translateToESPPRCNode(int node) {
    return node == instance.getDepot() ? sink : node;
  }

  public int translateFromESPPRCNode(int node) {
    return node == sink ? instance.getDepot() : node;
  }

  public List<Integer> getReverseNeighborhood(int node) {
    return instance.getReverseNeighborhood(translateFromESPPRCNode(node));
  }
}
