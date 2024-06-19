package commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Instance {

    public static final String DEFAULT_DIR = Utils.RESOURCES_PATH;
    private static final String DEFAULT_GRAPH_FILENAME = "graph.txt";
    private static final String DEFAULT_NEIGHBORS_FILENAME = "neighbors.txt";
    private static final String DEFAULT_PACKAGES_FILENAME = "packages.txt";
    private static final String DEFAULT_PARAMS_FILENAME = "params.txt";
    private static final String CAPACITY_STRING = "capacity";
    private static final String DEPOT_STRING = "depot";
    private static final String VEHICLES_STRING = "vehicles";
    private final String name;
    private final int numberOfNodes;
    private final int numberOfVehicles;
    private final int capacity;
    private final int depot;
    private final List<Customer> customers;
    private final Graph<StarRoutingGraph.SRNode, StarRoutingGraph.SREdge> graph;
    private final boolean allowUnusedVehicles;

    public Instance(String instanceName, String graphFilename, String neighborsFilename, String packagesFilename,
                    String paramsFilename, boolean allowUnusedVehicles) {
        this.name = instanceName;
        this.allowUnusedVehicles = allowUnusedVehicles;

        List<List<Integer>> adjacencyMatrix = Utils.parseIntegerMatrix(getFullPath(instanceName, graphFilename));
        this.numberOfNodes = getNumberOfNodes(adjacencyMatrix);
        this.graph = createGraph(adjacencyMatrix);

        Map<String, Integer> parameterValues = Utils.parseStringToIntMap(getFullPath(instanceName, paramsFilename));
        this.capacity = parameterValues.get(CAPACITY_STRING);
        this.numberOfVehicles = parameterValues.get(VEHICLES_STRING);
        this.depot = parameterValues.get(DEPOT_STRING) - 1;

        List<List<Integer>> customersAndDemand = Utils.parseIntegerMatrix(getFullPath(instanceName, packagesFilename));
        this.customers = createCustomerList(customersAndDemand);


        List<List<Integer>> neighbors = Utils.parseIntegerMatrix(getFullPath(instanceName, neighborsFilename));
        createNeighborsMap(neighbors, this.customers);

        this.reverseNeighborhoods = computeReverseNeighborhoods(this.numberOfNodes, this.customers, this.neighbors);

        checkRep();
    }

    public Instance(String instanceName) {
        this(instanceName, DEFAULT_GRAPH_FILENAME, DEFAULT_NEIGHBORS_FILENAME, DEFAULT_PACKAGES_FILENAME,
                DEFAULT_PARAMS_FILENAME, false);
    }

    public Instance(String instanceName, boolean allowUnusedVehicles) {
        this(instanceName, DEFAULT_GRAPH_FILENAME, DEFAULT_NEIGHBORS_FILENAME, DEFAULT_PACKAGES_FILENAME,
                DEFAULT_PARAMS_FILENAME, allowUnusedVehicles);
    }

    private static String getFullPath(String instance, String filename) {
        return DEFAULT_DIR + instance + "/" + filename;
    }

    private static int getNumberOfNodes(List<List<Integer>> adjacencyMatrix) {
        int maxNodeIndexFound = 0;
        for (List<Integer> line : adjacencyMatrix) {
            maxNodeIndexFound = Math.max(maxNodeIndexFound, line.get(0) - 1);
            maxNodeIndexFound = Math.max(maxNodeIndexFound, line.get(1) - 1);
        }
        return maxNodeIndexFound + 1;
    }

    private static Graph<StarRoutingGraph.SRNode, StarRoutingGraph.SREdge> createGraph(List<List<Integer>> adjacencyMatrix) {
        Graph<StarRoutingGraph.SRNode, StarRoutingGraph.SREdge> graph = new Graph<>();

        StarRoutingGraph.SRNode[] nodes = new StarRoutingGraph.SRNode[adjacencyMatrix.size()];
        for (int i = 0; i < adjacencyMatrix.size(); i++) {
            nodes[i] = new StarRoutingGraph.SRNode(i);
            graph.addNode(nodes[i]);
        }

        for (List<Integer> line : adjacencyMatrix) {
            int i = line.get(0) - 1;
            int j = line.get(1) - 1;
            int weight = line.get(2);
            if (weight >= 0 && i != j) {
                graph.addEdge(new StarRoutingGraph.SREdge(nodes[i], nodes[j], weight));
            }
        }
        return graph;
    }

    private static Map<Integer, Integer> createDemandMap(List<List<Integer>> customersAndDemand) {
        Map<Integer, Integer> customerToDemand = new HashMap<>();
        for (List<Integer> line : customersAndDemand) {
            int customer = line.get(0) - 1;
            int demand = line.get(1);
            customerToDemand.put(customer, demand);
        }
        return customerToDemand;
    }

    private static List<Customer> createCustomerList(List<List<Integer>> customersAndDemand) {
        List<Customer> customers = new ArrayList<>();
        int count = 0;
        for (List<Integer> line : customersAndDemand) {
            int customer = line.get(0) - 1;
            int demand = line.get(1);
            customers.add(new Customer(count, customer, demand));
            count++;
        }
        return customers;
    }

    private static void createNeighborsMap(List<List<Integer>> customerAndNeighbor, List<Customer> customers) {
        Map<Integer, Set<Integer>> neighbors = new HashMap<>();
        for (List<Integer> line : customerAndNeighbor) {
            int customer = line.get(0) - 1;
            int neighbor = line.get(1) - 1;
            neighbors.putIfAbsent(customer, new HashSet<>());
            if (!neighbors.containsKey(customer)) {
                System.out.printf("Trying to add non existing customer: %d %n", customer);
            } else {
                neighbors.get(customer).add(neighbor);
            }
        }
        for (Customer c : customers) {
            for (int neighbor : neighbors.get(c.getId())) {
                c.addNeighbor(neighbor);
            }
        }
    }

    private static List<List<Integer>> computeReverseNeighborhoods(int numberOfNodes, List<Integer> customers,
                                                                   Map<Integer, Set<Integer>> neighbors) {
        List<List<Integer>> ret = new ArrayList<>();
        for (int node = 0; node < numberOfNodes; node++) {
            ret.add(new ArrayList<>());
            for (int customer : customers) {
                if (neighbors.get(customer).contains(node)) {
                    ret.get(node).add(customer);
                }
            }
        }
        return ret;
    }

    private void checkRep() {
        assert customers.size() == demand.size();
        assert customers.size() == neighbors.size();
        assert customers.size() < numberOfNodes;
        for (Customer customer : customers) {
            assert demand.containsKey(customer);
            assert neighbors.containsKey(customer);
            assert customer != depot;
            assert 0 <= customer;
            assert customer < numberOfNodes;
        }
        assert graph.getSize() == numberOfNodes;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getDepot() {
        return depot;
    }

    public Integer getEdgeWeight(int i, int j) {
        assert graph.containsEdge(i, j);
        return graph.getEdge(i, j).getWeight();
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public int getNumberOfCustomers() {
        return customers.size();
    }

    public int getCustomer(int index) {
        return customers.get(index);
    }

    public Set<Integer> getNeighbors(int customer) {
        return neighbors.get(customer);
    }

    public Integer getDemand(int customer) {
        return demand.get(customer);
    }

    public boolean unusedVehiclesAllowed() {
        return allowUnusedVehicles;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getReverseNeighborhood(int node) {
        return reverseNeighborhoods.get(node);
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public String toString() {
        return "Instance{" + "numberOfNodes=" + numberOfNodes + ", numberOfVehicles=" + numberOfVehicles +
                ", capacity=" + capacity + ", depot=" + depot + ", customers=" + customers + ", neighbors=" +
                neighbors + ", demand=" + demand + ", allowUnusedVehicles=" + allowUnusedVehicles + '}';
    }

}