package algorithm;

import commons.FeasiblePath;
import commons.Instance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RearrangeCustomersHeuristic {

    private final Instance instance;

    public RearrangeCustomersHeuristic(Instance instance) {
        this.instance = instance;
    }

    private Set<Integer> potentialCustomers(FeasiblePath path) {
        Set<Integer> ret = new TreeSet<>(Comparator.comparingInt(instance::getDemand));
        for (int node : path.getNodes()) {
            ret.addAll(instance.getReverseNeighborhood(node));
        }
        return ret;
    }

    public List<FeasiblePath> run(List<FeasiblePath> paths) {
        List<FeasiblePath> ret = new ArrayList<>();
        boolean[] visited = new boolean[instance.getNumberOfNodes()];
        int numberOfVisitedCustomers = 0;
        paths.sort(Comparator.comparing(FeasiblePath::getCost));
        for (FeasiblePath path : paths) {
            if (numberOfVisitedCustomers < instance.getNumberOfCustomers()) {
                int cumulativeDemand = 0;
                FeasiblePath currentPath = path.getCopyWithoutCustomers();
                List<Integer> currentCustomers = new ArrayList<>();
                for (int customer : potentialCustomers(path)) {
                    if (!visited[customer]) {
                        cumulativeDemand += instance.getDemand(customer);
                        if (cumulativeDemand > instance.getCapacity()) {
                            currentPath.addCustomers(currentCustomers);
                            currentCustomers = new ArrayList<>();
                            ret.add(currentPath);
                            cumulativeDemand = instance.getDemand(customer);
                        }
                        currentCustomers.add(customer);
                        visited[customer] = true;
                        numberOfVisitedCustomers++;
                    }
                }
            } else {
                break;
            }
        }
        return ret;
    }
}
