package algorithm.pricing;

import java.util.List;

public interface LabelDump {

    void addLabel(Label label);

    boolean dominates(Label label);

    List<Label> getNegativeReducedCostLabels(int node);
}
