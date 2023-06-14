import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Data {

    private static final String DELIMITER = " ";

    private static final String DEFAULT_DIR = "src/resources/";
    private static final String DEFAULT_GRAPH_FILENAME = "graph.txt";
    private static final String DEFAULT_CUSTOMERS_FILENAME = "customers.txt";
    private static final String DEFAULT_PACKAGES_FILENAME = "packages.txt";
    private static final String DEFAULT_PARAMS_FILENAME = "params.txt";

    private static final String CAPACITY_STRING = "capacity";
    private static final String DEPOT_STRING = "depot";
    private static final String VEHICLES_STRING = "vehicles";

    public final int nodes;
    public final int vehicles;
    public final int capacity;
    public final int depot;
    public final int[][] graphWeights;
    public final int[] customers;
    public final Map<Integer, Set<Integer>> neighbors;
    public final Map<Integer, Integer> demand;

    public Data(String instance, String graphFilename, String customersFilename, String packagesFilename,
            String paramsFilename) throws IOException {

        this.nodes = getNumberOfNodes(getFullPath(instance, graphFilename));
        this.graphWeights = new int[this.nodes][this.nodes];
        fillGraphWeights(getFullPath(instance, graphFilename));
        this.capacity = Objects.requireNonNull(
                getParameterValue(CAPACITY_STRING, getFullPath(instance, paramsFilename)));
        this.vehicles = Objects.requireNonNull(
                getParameterValue(VEHICLES_STRING, getFullPath(instance, paramsFilename)));
        this.depot = Objects.requireNonNull(
                getParameterValue(DEPOT_STRING, getFullPath(instance, paramsFilename))) - 1;
        this.customers = new int[getNumberOfCustomers(getFullPath(instance, packagesFilename))];
        this.demand = new HashMap<Integer, Integer>();
        this.fillCustomersAndDemand(getFullPath(instance, packagesFilename));
        this.neighbors = new HashMap<Integer, Set<Integer>>();
                 this.fillCustomerNeighbors(getFullPath(instance, customersFilename));

        System.out.println(demand);
        System.out.println(neighbors);
        System.out.println(Arrays.toString(customers));
        System.out.println(Arrays.deepToString(graphWeights));
    }

    public Data(String instance) throws IOException {
        this(instance, DEFAULT_GRAPH_FILENAME, DEFAULT_CUSTOMERS_FILENAME,
                DEFAULT_PACKAGES_FILENAME, DEFAULT_PARAMS_FILENAME);
    }

    private String getFullPath(String instance, String filename) {
        return DEFAULT_DIR + instance + "/" + filename;
    }

    private List<Integer> readIntegerLine(String line, String delimiter) {
        return Arrays.stream(line.split(delimiter))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    private int getNumberOfNodes(String graphFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(graphFilename));
        String line;
        int maxNodeIndexFound = 0;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line, DELIMITER);
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
            List<Integer> values = readIntegerLine(line, DELIMITER);
            this.graphWeights[values.get(0) - 1][values.get(1) - 1] = values.get(2);
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
            List<Integer> values = readIntegerLine(line, DELIMITER);
            int customer = values.get(0) - 1;
            int demand = values.get(1);
            this.demand.put(customer, demand);
            this.customers[index] = customer;
            index++;
        }
        br.close();
    }

   private  void fillCustomerNeighbors(String customersFilename) throws IOException {
        for (int customer : this.customers) {
            this.neighbors.put(customer, new HashSet<Integer>());
            this.neighbors.get(customer).add(customer);
        }
        BufferedReader br = new BufferedReader(new FileReader(customersFilename));
        String line;
        while ((line = br.readLine()) != null) {
            List<Integer> values = readIntegerLine(line, DELIMITER);
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

}