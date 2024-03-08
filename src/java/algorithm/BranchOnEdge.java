package algorithm;

import commons.FeasiblePath;

public class BranchOnEdge implements BranchingDirection {

    private final int start;
    private final int end;
    private final int bound;
    private final boolean isLowerBound;

    public BranchOnEdge(int start, int end, int bound, boolean isLowerBound) {
        this.start = start;
        this.end = end;
        this.bound = bound;
        this.isLowerBound = isLowerBound;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getBound() {
        return bound;
    }

    public boolean isLowerBound() {
        return isLowerBound;
    }

    @Override
    public boolean isCompatible(FeasiblePath path) {
        if (isLowerBound || bound > 0) {
            return true;
        }
        return !path.containsEdge(start, end);
    }

    @Override
    public String toString() {
        return "BranchOnEdge{" + "start=" + start + ", end=" + end + ", bound=" + bound + ", isLowerBound=" +
                isLowerBound + '}';
    }
}
