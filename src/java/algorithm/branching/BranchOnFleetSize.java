package algorithm.branching;

import commons.Route;

public class BranchOnFleetSize implements Branch {

    private final int bound;
    private final Direction direction;

    public BranchOnFleetSize(int bound, Direction direction) {
        this.bound = bound;
        this.direction = direction;
    }

    @Override
    public boolean isCompatible(Route path) {
        return true;
    }

    @Override
    public int getBound() {
        return bound;
    }

    @Override
    public boolean isUpperBound() {
        return Direction.DOWN.equals(direction);
    }

    @Override
    public boolean isLowerBound() {
        return Direction.UP.equals(direction);
    }
}
