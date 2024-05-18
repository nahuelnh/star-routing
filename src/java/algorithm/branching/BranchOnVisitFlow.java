package algorithm.branching;

import commons.FeasiblePath;
import commons.Graph;

public class BranchOnVisitFlow implements Branch {
    private final Graph.Edge edge;
    private final int customer;
    private final int bound;
    private final Direction direction;

    public BranchOnVisitFlow(Graph.Edge edge, int customer, int bound, Direction direction) {
        this.edge = edge;
        this.customer = customer;
        this.bound = bound;
        this.direction = direction;
    }

    public Graph.Edge getEdge() {
        return edge;
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
        if (isUpperBound() && bound == 0) {
            return !(path.isCustomerServed(customer) && path.containsEdge(edge.getStart(), edge.getEnd()));
        }
        if (isLowerBound() && bound == 1) {
            return !path.isCustomerServed(customer) || path.containsEdge(edge.getStart(), edge.getEnd());
        }
        return true;
    }

    @Override
    public String toString() {
        return "BranchOnVisitFlow{" + "start=" + edge.getStart() + ", end=" + edge.getEnd() + ", customer=" + customer +
                ", bound=" + bound + ", direction=" + direction + '}';
    }

}
