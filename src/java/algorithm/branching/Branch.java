package algorithm.branching;

import commons.FeasiblePath;

public interface Branch {

    boolean isCompatible(FeasiblePath path);

    int getBound();

    boolean isUpperBound();

    boolean isLowerBound();

    enum Direction {
        UP,
        DOWN
    }
}
