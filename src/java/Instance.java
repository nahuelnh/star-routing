import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Instance {


    private static final String DEFAULT_DIR = "src/resources/";
    private static final String DEFAULT_GRAPH_FILENAME = "graph.txt";
    private static final String DEFAULT_NEIGHBORS_FILENAME = "customers.txt";
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

    public Instance(String instance, String graphFilename, String neighborsFilename, String packagesFilename,
                    String paramsFilename) {
        this.nodes = getNumberOfNodes(getFullPath(instance, graphFilename));
        this.graphWeights = createWeightsMatrix(this.nodes, getFullPath(instance, graphFilename));

        this.capacity = getParameterValue(CAPACITY_STRING, getFullPath(instance, paramsFilename));
        this.vehicles = getParameterValue(VEHICLES_STRING, getFullPath(instance, paramsFilename));
        this.depot = getParameterValue(DEPOT_STRING, getFullPath(instance, paramsFilename)) - 1;

        this.customers = createCustomerList(getFullPath(instance, packagesFilename));
        this.demand = createDemandMap(getFullPath(instance, packagesFilename));
        this.neighbors = createNeighborsMap(getFullPath(instance, neighborsFilename), this.customers);

        checkRep();

        System.out.println("Demand: " + this.demand);
        System.out.println("Neighbors: " + this.neighbors);
        System.out.println("Customer Set: " + this.customers);
        System.out.println("Weights: " + this.graphWeights);
    }

    public Instance(String instance) throws IOException {
        this(instance, DEFAULT_GRAPH_FILENAME, DEFAULT_NEIGHBORS_FILENAME,
                DEFAULT_PACKAGES_FILENAME, DEFAULT_PARAMS_FILENAME);
    }

    private static List<List<Integer>> createWeightsMatrix(int size, String graphFilename) {
        List<List<Integer>> matrix = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            matrix.add(new ArrayList<>(Collections.nCopies(size, Integer.MAX_VALUE)));
        }
        try (BufferedReader br = new BufferedReader(new FileReader(graphFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                int i = values.get(0) - 1;
                int j = values.get(1) - 1;
                int weight = values.get(2);
                if (i != j) {
                    matrix.get(i).set(j, weight);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matrix;
    }

    private static List<Integer> readIntegerLine(String line) {
        return Arrays.stream(line.split(DELIMITER))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    private static Map<Integer, Integer> createDemandMap(String packagesFilename) {
        Map<Integer, Integer> customerToDemand = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(packagesFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                int customer = values.get(0) - 1;
                int demand = values.get(1);
                customerToDemand.put(customer, demand);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return customerToDemand;
    }

    private static List<Integer> createCustomerList(String packagesFilename) {
        List<Integer> customers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(packagesFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                int customer = values.get(0) - 1;
                customers.add(customer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return customers.stream().distinct().toList();
    }

    private static String getFullPath(String instance, String filename) {
        return DEFAULT_DIR + instance + "/" + filename;
    }

    private static Integer getParameterValue(String parameterName, String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                if (parameterName.equals(values[0])) {
                    return Integer.valueOf(values[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new AssertionError("Parameter not found: " + parameterName + " in: " + filename);
    }

    private static Map<Integer, Set<Integer>> createNeighborsMap(String customersFilename, List<Integer> customers) {
        Map<Integer, Set<Integer>> neighbors = new HashMap<>();
        for (int customer : customers) {
            neighbors.put(customer, new HashSet<>());
            neighbors.get(customer).add(customer);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(customersFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                int customer = values.get(0) - 1;
                int neighbor = values.get(1) - 1;
                if (!neighbors.containsKey(customer)) {
                    System.out.printf("Trying to add non existing customer: %d %n", customer);
                } else {
                    neighbors.get(customer).add(neighbor);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return neighbors;
    }

    private static int getNumberOfNodes(String graphFilename) {
        int maxNodeIndexFound = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(graphFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                maxNodeIndexFound = Math.max(maxNodeIndexFound, values.get(0) - 1);
                maxNodeIndexFound = Math.max(maxNodeIndexFound, values.get(1) - 1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return maxNodeIndexFound + 1;
    }

    private void checkRep() {
        assert customers.size() == demand.size();
        assert customers.size() == neighbors.size();
        assert customers.size() < nodes;
        for (int customer : customers) {
            assert demand.containsKey(customer);
            assert neighbors.containsKey(customer);
            assert customer != depot;
            assert 0 <= customer;
            assert customer < nodes;
        }
        assert graphWeights.size() == nodes;
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