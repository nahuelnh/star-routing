package algorithm.branching;

import commons.Route;

public interface Branch {

    boolean isCompatible(Route path);

    int getBound();

    boolean isUpperBound();

    boolean isLowerBound();

    enum Direction {
        UP,
        DOWN
    }
}
