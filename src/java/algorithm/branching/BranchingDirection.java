package algorithm.branching;

import commons.FeasiblePath;

public interface BranchingDirection {
    boolean isCompatible(FeasiblePath path);
}
