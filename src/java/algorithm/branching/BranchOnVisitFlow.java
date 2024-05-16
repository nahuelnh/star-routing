package algorithm.branching;

import commons.FeasiblePath;

public class BranchOnVisitFlow implements Branch {
    private final int start;
    private final int end;
    private final int customer;
    private final int bound;
    private final Direction direction;

    public BranchOnVisitFlow(int start, int end, int customer, int bound, Direction direction) {
        this.start = start;
        this.end = end;
        this.customer = customer;
        this.bound = bound;
        this.direction = direction;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getCustomer() {
        return customer;
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
        return !(path.containsEdge(start, end) && path.isCustomerServed(customer));
    }

    @Override
    public String toString() {
        return "BranchOnVisitFlow{" + "start=" + start + ", end=" + end + ", customer=" + customer + ", bound=" +
                bound + ", direction=" + direction + '}';
    }

}
