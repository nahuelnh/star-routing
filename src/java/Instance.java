import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Instance {

    private static final String DEFAULT_DIR = "src/resources/";
    private static final String DEFAULT_GRAPH_FILENAME = "graph.txt";
    private static final String DEFAULT_NEIGHBORS_FILENAME = "customers.txt";
    private static final String DEFAULT_PACKAGES_FILENAME = "packages.txt";
    private static final String DEFAULT_PARAMS_FILENAME = "params.txt";
    private static final String CAPACITY_STRING = "capacity";
    private static final String DEPOT_STRING = "depot";
    private static final String VEHICLES_STRING = "vehicles";
    private final int numberOfNodes;
    private final int numberOfVehicles;
    private final int capacity;
    private final int depot;
    private final List<List<Integer>> graphWeights;
    private final List<Integer> customers;
    private final Map<Integer, Set<Integer>> neighbors;
    private final Map<Integer, Integer> demand;

    public Instance(String instanceName, String graphFilename, String neighborsFilename, String packagesFilename, String paramsFilename) {
        List<List<Integer>> adjacencyMatrix = Utils.parseIntegerMatrix(getFullPath(instanceName, graphFilename));
        this.numberOfNodes = getNumberOfNodes(adjacencyMatrix);
        this.graphWeights = createWeightsMatrix(adjacencyMatrix);

        Map<String, Integer> parameterValues = Utils.parseStringToIntMap(getFullPath(instanceName, paramsFilename));
        this.capacity = parameterValues.get(CAPACITY_STRING);
        this.numberOfVehicles = parameterValues.get(VEHICLES_STRING);
        this.depot = parameterValues.get(DEPOT_STRING) - 1;

        List<List<Integer>> customersAndDemand = Utils.parseIntegerMatrix(getFullPath(instanceName, packagesFilename));
        this.customers = createCustomerList(customersAndDemand);
        this.demand = createDemandMap(customersAndDemand);

        List<List<Integer>> neighbors = Utils.parseIntegerMatrix(getFullPath(instanceName, neighborsFilename));
        this.neighbors = createNeighborsMap(neighbors, this.customers);

        checkRep();

        System.out.println("Demand: " + this.demand);
        System.out.println("Neighbors: " + this.neighbors);
        System.out.println("Customer Set: " + this.customers);
        System.out.println("Weights: " + this.graphWeights);
    }

    public Instance(String instanceName) {
        this(instanceName, DEFAULT_GRAPH_FILENAME, DEFAULT_NEIGHBORS_FILENAME, DEFAULT_PACKAGES_FILENAME, DEFAULT_PARAMS_FILENAME);
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

    private static List<List<Integer>> createWeightsMatrix(List<List<Integer>> adjacencyMatrix) {
        int size = adjacencyMatrix.size();
        List<List<Integer>> weightsMatrix = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            weightsMatrix.add(new ArrayList<>(Collections.nCopies(size, Integer.MAX_VALUE)));
        }
        for (List<Integer> line : adjacencyMatrix) {
            int i = line.get(0) - 1;
            int j = line.get(1) - 1;
            int weight = line.get(2);
            if (i != j) {
                weightsMatrix.get(i).set(j, weight);
            }
        }
        return weightsMatrix;
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

    private static List<Integer> createCustomerList(List<List<Integer>> customersAndDemand) {
        List<Integer> customers = new ArrayList<>();
        for (List<Integer> line : customersAndDemand) {
            int customer = line.get(0) - 1;
            customers.add(customer);
        }
        return customers.stream().distinct().sorted().toList();
    }

    private static Map<Integer, Set<Integer>> createNeighborsMap(List<List<Integer>> customersAndDemand, List<Integer> customers) {
        Map<Integer, Set<Integer>> neighbors = new HashMap<>();
        for (int customer : customers) {
            neighbors.put(customer, new HashSet<>());
            neighbors.get(customer).add(customer);
        }
        for (List<Integer> line : customersAndDemand) {
            int customer = line.get(0) - 1;
            int neighbor = line.get(1) - 1;
            if (!neighbors.containsKey(customer)) {
                System.out.printf("Trying to add non existing customer: %d %n", customer);
            } else {
                neighbors.get(customer).add(neighbor);
            }
        }
        return neighbors;
    }

    private void checkRep() {
        assert customers.size() == demand.size();
        assert customers.size() == neighbors.size();
        assert customers.size() < numberOfNodes;
        for (int customer : customers) {
            assert demand.containsKey(customer);
            assert neighbors.containsKey(customer);
            assert customer != depot;
            assert 0 <= customer;
            assert customer < numberOfNodes;
        }
        assert graphWeights.size() == numberOfNodes;
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

    public Integer getWeight(int i, int j) {
        return graphWeights.get(i).get(j);
    }

    public List<Integer> getCustomers() {
        return customers;
    }

    public int getNumberOfCustomers() {
        return customers.size();
    }

    public int getCustomer(int index) {
        return customers.get(index);
    }

    public Set<Integer> getNeighbors(int node) {
        return neighbors.get(node);
    }

    public Integer getDemand(int customer) {
        return demand.get(customer);
    }
}