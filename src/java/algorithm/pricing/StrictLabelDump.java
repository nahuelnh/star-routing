package algorithm.pricing;

import commons.Utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StrictLabelDump implements LabelDump {

    private static final double EPSILON = 1e-6d;
    private final List<Map<BitSet, Map<BitSet, Label>>> dump;

    public StrictLabelDump(int size) {
        dump = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dump.add(new HashMap<>());
        }
    }

    @Override
    public void addLabel(Label l) {
        Map<BitSet, Map<BitSet, Label>> currentNodeMap = dump.get(l.node());
        currentNodeMap.putIfAbsent(l.visitedNodes(), new HashMap<>());
        currentNodeMap.get(l.visitedNodes()).put(l.visitedCustomers(), l);

    }

    @Override
    public boolean dominates(Label l) {
        for (BitSet visitedNodes : dump.get(l.node()).keySet()) {
            if (Utils.isSubset(visitedNodes, l.visitedNodes())) {
                Map<BitSet, Label> bucket = dump.get(l.node()).get(visitedNodes);
                for (BitSet visitedCustomers : bucket.keySet()) {
                    if (Utils.isSubset(visitedCustomers, l.visitedCustomers())) {
                        if (bucket.get(visitedCustomers).cost() + EPSILON < l.cost()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Label> getNegativeReducedCostLabels(int node) {
        List<Label> ret = new ArrayList<>();
        for (BitSet visitedNodes : dump.get(node).keySet()) {
            Map<BitSet, Label> bucket = dump.get(node).get(visitedNodes);
            for (BitSet visitedCustomers : bucket.keySet()) {
                Label currentLabel = bucket.get(visitedCustomers);
                if (currentLabel.cost() < -EPSILON) {
                    ret.add(currentLabel);
                }
            }
        }
        return ret;
    }
}
