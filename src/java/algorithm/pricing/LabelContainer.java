package algorithm.pricing;

import java.util.List;

public interface LabelContainer {

    void addLabel(Label label);

    boolean dominates(Label label);

    List<Label> getNegativeReducedCostLabels();

    List<Label> getLabels();
}
