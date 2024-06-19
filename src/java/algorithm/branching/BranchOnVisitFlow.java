package algorithm.branching;

import commons.FeasiblePath;
import commons.Graph;
import commons.StarRoutingGraph;

public class BranchOnVisitFlow implements Branch {
    private final StarRoutingGraph.SRNode startNode;
    private final StarRoutingGraph.SRNode endNode;
    private final int customer;
    private final int bound;
    private final Direction direction;

    public BranchOnVisitFlow(StarRoutingGraph.SRNode startNode, StarRoutingGraph.SRNode endNode, int customer, int bound, Direction direction) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.customer = customer;
        this.bound = bound;
        this.direction = direction;
    }

    public StarRoutingGraph.SRNode getStartNode() {
        return startNode;
    }

    public StarRoutingGraph.SRNode getEndNode() {
        return endNode;
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
