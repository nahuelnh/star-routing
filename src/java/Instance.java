import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Instance {

    private static final String DELIMITER = " ";

    private static final String DEFAULT_DIR = "src/resources/";
    private static final String DEFAULT_GRAPH_FILENAME = "graph.txt";
    private static final String DEFAULT_CUSTOMERS_FILENAME = "customers.txt";
    private static final String DEFAULT_PACKAGES_FILENAME = "packages.txt";
    private static final String DEFAULT_PARAMS_FILENAME = "params.txt";

    private static final String CAPACITY_STRING = "capacity";
    private static final String DEPOT_STRING = "depot";
    private static final String VEHICLES_STRING = "vehicles";

    private final int nodes;
    private final int vehicles;
    private final int capacity;
    private final int depot;
    private final List<List<Integer>> graphWeights;
    private final List<Integer> customers;
    private final Map<Integer, Set<Integer>> neighbors;
    private final Map<Integer, Integer> demand;

    public Instance(String instance, String graphFilename, String customersFilename, String packagesFilename,
                    String paramsFilename) throws IOException {
        this.nodes = getNumberOfNodes(getFullPath(instance, graphFilename));
        this.graphWeights = new ArrayList<>(this.nodes);
        for (int i = 0; i < this.nodes; i++) {
            this.graphWeights.add(new ArrayList<>(Collections.nCopies(this.nodes, 0)));
        }
        fillGraphWeights(getFullPath(instance, graphFilename));
        this.capacity = Objects.requireNonNull(
                getParameterValue(CAPACITY_STRING, getFullPath(instance, paramsFilename)));
        this.vehicles = Objects.requireNonNull(
                getParameterValue(VEHICLES_STRING, getFullPath(instance, paramsFilename)));
        this.depot = Objects.requireNonNull(
                getParameterValue(DEPOT_STRING, getFullPath(instance, paramsFilename))) - 1;
        int numberOfCustomers = getNumberOfCustomers(getFullPath(instance, packagesFilename));
        this.customers = new ArrayList<>(Collections.nCopies(numberOfCustomers, 0));
        this.demand = new HashMap<>();
        this.fillCustomersAndDemand(getFullPath(instance, packagesFilename));
        this.neighbors = new HashMap<>();
        this.fillCustomerNeighbors(getFullPath(instance, customersFilename));

        System.out.println(this.demand);
        System.out.println(this.neighbors);
        System.out.println(this.customers);
        System.out.println(this.graphWeights);
    }

    public Instance(String instance) throws IOException {
        this(instance, DEFAULT_GRAPH_FILENAME, DEFAULT_CUSTOMERS_FILENAME,
                DEFAULT_PACKAGES_FILENAME, DEFAULT_PARAMS_FILENAME);
    }

    private String getFullPath(String instance, String filename) {
        return DEFAULT_DIR + instance + "/" + filename;
    }

    private List<Integer> readIntegerLine(String line) {
        return Arrays.stream(line.split(DELIMITER))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    private int getNumberOfNodes(String graphFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(graphFilename));
        String line;
        int maxNodeIndexFound = 0;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line);
            maxNodeIndexFound = Math.max(maxNodeIndexFound,
                    Math.max(values.get(0), values.get(1)));
        }
        br.close();
        return maxNodeIndexFound;
    }

    private Integer getParameterValue(String parameterName, String paramsFilename)
            throws NumberFormatException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(paramsFilename));
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(DELIMITER);
            if (parameterName.equals(values[0])) {
                br.close();
                return Integer.valueOf(values[1]);
            }
        }
        br.close();
        return null;
    }

    private void fillGraphWeights(String graphFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(graphFilename));
        String line;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line);
            this.graphWeights.get(values.get(0) - 1).set(values.get(1) - 1, values.get(2));
        }
        br.close();
    }

    private int getNumberOfCustomers(String packagesFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(packagesFilename));
        int lines = (int) br.lines().count();
        br.close();
        return lines;
    }

    private void fillCustomersAndDemand(String packagesFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(packagesFilename));
        String line;
        int index = 0;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line);
            int customer = values.get(0) - 1;
            int demand = values.get(1);
            this.demand.put(customer, demand);
            this.customers.set(index, customer);
            index++;
        }
        br.close();
    }

    private void fillCustomerNeighbors(String customersFilename) throws IOException {
        for (int customer : this.customers) {
            this.neighbors.put(customer, new HashSet<Integer>());
            this.neighbors.get(customer).add(customer);
        }
        BufferedReader br = new BufferedReader(new FileReader(customersFilename));
        String line;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line);
            int customer = values.get(0) - 1;
            int neighbor = values.get(1) - 1;
            if (!this.neighbors.containsKey(customer)) {
                br.close();
                throw new AssertionError("Non existing customer:" + customer);
            }
            this.neighbors.get(customer).add(neighbor);
        }
        br.close();
    }

    public int getNodes() {
        return nodes;
    }

    public int getVehicles() {
        return vehicles;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getDepot() {
        return depot;
    }

    public Integer getGraphWeights(int i, int j) {
        return graphWeights.get(i).get(j);
    }

    public List<Integer> getCustomers() {
        return customers;
    }

    public Map<Integer, Set<Integer>> getNeighbors() {
        return neighbors;
    }

    public Integer getDemand(int customer) {
        return demand.get(customer);
    }
}