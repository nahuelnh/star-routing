package algorithm.pricing;

import commons.Utils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

class RelaxedLabelContainer implements LabelContainer {

  private static final double EPSILON = Utils.DEFAULT_EPSILON;

  private final Label[]     labels;
  private final SegmentTree tree;

  public RelaxedLabelContainer(int capacity) {
    this.labels = new Label[capacity + 1];
    this.tree = new SegmentTree(filledArray(capacity + 1));
  }

  private static double[] filledArray(int size) {
    double[] arr = new double[size];
    Arrays.fill(arr, Double.MAX_VALUE);
    return arr;
  }

  @Override
  public void addLabel(Label l) {
    labels[l.demand()] = l;
    tree.update(l.demand(), l.cost());
  }

  @Override
  public boolean dominates(Label l) {
    return tree.query(0, l.demand() + 1) < l.cost();
  }

  @Override
  public List<Label> getNegativeReducedCostLabels() {
    return Arrays.stream(labels)
        .filter(Objects::nonNull)
        .filter(label -> label.cost() < -EPSILON)
        .toList();
  }

  @Override
  public List<Label> getLabels() {
    return Arrays.stream(labels).sorted(Comparator.comparing(Label::cost)).toList();
  }
}
