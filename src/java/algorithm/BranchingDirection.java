package algorithm;

import commons.FeasiblePath;

public interface BranchingDirection {
    boolean isCompatible(FeasiblePath path);
}
