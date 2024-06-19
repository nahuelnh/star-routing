package commons;

import java.util.HashSet;
import java.util.Set;

public class StarRoutingGraph extends Graph<StarRoutingGraph.SRNode> {

    public StarRoutingGraph() {
        createGraph();
    }

    private void createGraph() {

    }

    public static class SRNode extends Graph.Node {

        private final Set<Integer> reverseNeighborhood;

        public SRNode(int id) {
            super(id);
            this.reverseNeighborhood = new HashSet<>();
        }


        public void addNeighbor(int customer) {
            reverseNeighborhood.add(customer);
        }

        public Set<Integer> getReverseNeighborhood() {
            return reverseNeighborhood;
        }
    }


}
