package algorithm.branching;

import commons.FeasiblePath;

public class BranchOnEdge implements Branch {

    private final int start;
    private final int end;
    private final int bound;
    private final Direction direction;

    public BranchOnEdge(int start, int end, int bound, Direction direction) {
        this.start = start;
        this.end = end;
        this.bound = bound;
        this.direction = direction;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
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

    @Override
    public boolean isCompatible(FeasiblePath path) {
        if (isLowerBound() || bound > 0) {
            return true;
        }
        return !path.containsEdge(start, end);
    }

    @Override
    public String toString() {
        return "BranchOnEdge{" + "start=" + start + ", end=" + end + ", bound=" + bound + ", direction=" + direction +
                '}';
    }
}
