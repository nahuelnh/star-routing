package algorithm.pricing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RelaxedLabelDump implements LabelDump {

    private static final double EPSILON = 1e-6d;
    private final Label[][] labels;
    private final MinSegmentTree[] trees;

    public RelaxedLabelDump(int numberOfNodes, int capacity) {
        this.labels = new Label[numberOfNodes][capacity + 1];
        this.trees = new MinSegmentTree[numberOfNodes];
        double[] arr = new double[capacity + 1];
        Arrays.fill(arr, Double.MAX_VALUE);
        for (int i = 0; i < numberOfNodes; i++) {
            trees[i] = new MinSegmentTree(arr);
        }
    }

    @Override
    public void addLabel(Label l) {
        labels[l.node()][l.demand()] = l;
        trees[l.node()].update(l.demand(), l.cost());
    }

    @Override
    public boolean dominates(Label l) {
        return trees[l.node()].query(0, l.demand() + 1) < l.cost();
    }

    @Override
    public List<Label> getNegativeReducedCostLabels(int node) {
        List<Label> ret = new ArrayList<>();
        for (int q = 0; q < labels[node].length; q++) {
            if (labels[node][q] != null && labels[node][q].cost() < -EPSILON) {
                ret.add(labels[node][q]);
            }
        }
        return ret;
    }
}
