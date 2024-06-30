package algorithm.pricing;

import commons.Utils;

import java.util.*;

class ExactLabelContainer implements LabelContainer {

  private static final double EPSILON = Utils.DEFAULT_EPSILON;

  private final Map<BitSet, Map<BitSet, Label>> container;

  public ExactLabelContainer() {
    this.container = new HashMap<>();
  }

  @Override
  public void addLabel(Label l) {
    container.putIfAbsent(l.visitedNodes(), new HashMap<>());
    container.get(l.visitedNodes()).put(l.visitedCustomers(), l);
  }

  @Override
  public boolean dominates(Label other) {
    for (BitSet visitedNodes : container.keySet()) {
      if (Utils.isSubset(visitedNodes, other.visitedNodes())) {
        Map<BitSet, Label> bucket = container.get(visitedNodes);
        for (BitSet visitedCustomers : bucket.keySet()) {
          if (Utils.isSubset(visitedCustomers, other.visitedCustomers())
              && bucket.get(visitedCustomers).cost() + EPSILON < other.cost()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public List<Label> getNegativeReducedCostLabels() {
    List<Label> ret = new ArrayList<>();
    for (BitSet visitedNodes : container.keySet()) {
      Map<BitSet, Label> bucket = container.get(visitedNodes);
      for (BitSet visitedCustomers : bucket.keySet()) {
        Label currentLabel = bucket.get(visitedCustomers);
        if (currentLabel.cost() < -EPSILON) {
          ret.add(currentLabel);
        }
      }
    }
    return ret;
  }

  @Override
  public List<Label> getLabels() {
    return container.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(Label::cost))
        .toList();
  }
}
