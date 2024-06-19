package algorithm.pricing;

import algorithm.RMPLinearSolution;
import commons.Graph;
import commons.Instance;

public class ESPPRCGraph extends Graph<ESPPRCGraph.ESPPRCNode> {

    private final Instance instance;
    private final RMPLinearSolution rmpSolution;
    private final ESPPRCNode source;
    private final ESPPRCNode sink;
    private int uniqueId;

    public ESPPRCGraph(Instance instance, RMPLinearSolution rmpSolution) {
        this.instance = instance;
        this.rmpSolution = rmpSolution;
        this.source = new ESPPRCNode(instance.getDepot(), -1);
        this.sink = new ESPPRCNode(instance.getDepot(), -1);
        this.uniqueId = 0;
        addNode(source);
        addNode(sink);
        createGraph();
    }

    private void createGraph() {

        // Add nodes
        for (int i = 0; i < instance.getNumberOfNodes(); i++) {
            if (i != instance.getDepot()) {
                addNode(new ESPPRCNode(i, -1));
                for (int j = 0; j < instance.getNumberOfCustomers(); j++) {
                    addNode(new ESPPRCNode(i, j));
                }
            }
        }


        for (ESPPRCNode i : getNodes()) {
            for (ESPPRCNode j : getNodes()) {
                if (i.getId() != j.getId() && i.getId() != sink.getId() && j.getId() != source.getId() && !(i.getId() == source.getId() && j.getId() == sink.getId())) {
                    double originalWeight = instance.getGraph().getEdge(i.getSrNodeId(), j.getSrNodeId()).getWeight();
                    addEdge(new ESPPRCEdge(i, j, getModifiedWeight(originalWeight, i.getCustomer(), j.getCustomer())));
                }
            }
        }
    }

    public ESPPRCNode getSource() {
        return source;
    }

    public ESPPRCNode getSink() {
        return sink;
    }

//    public int translateToESPPRCNode(int node) {
//        return node == instance.getDepot() ? sink : node;
//    }
//
//    public int translateFromESPPRCNode(ESPPRCNode node) {
//        return node == sink ? instance.getDepot() : node;
//    }

//    public List<Integer> getReverseNeighborhood(int node) {
//        return instance.getReverseNeighborhood(translateFromESPPRCNode(node));
//    }

    public class ESPPRCNode extends Node {

        private final int srNodeId;
        private final int customer;

        public ESPPRCNode(int srNodeId, int customer) {
            super(uniqueId);
            this.srNodeId = srNodeId;
            this.customer = customer;
            uniqueId++;
        }

        public int getSrNodeId() {
            return srNodeId;
        }

        public int getCustomer() {
            return customer;
        }
    }

    private double getModifiedWeight(double originalWeight, int customer1, int customer2) {
        double modifiedWeight = originalWeight;
        if (customer1 != -1) {
            modifiedWeight -= 0.5 * rmpSolution.getCustomerDual(customer1);
        }
        if (customer2 != -1) {
            modifiedWeight -= 0.5 * rmpSolution.getCustomerDual(customer2);
        }
        return modifiedWeight;
    }
}
