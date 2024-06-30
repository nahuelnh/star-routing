package algorithm;

import commons.Route;
import commons.Instance;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class RearrangeCustomersHeuristic {

    private static final double EPSILON = 1e-6;
    private final Instance instance;

    public RearrangeCustomersHeuristic(Instance instance) {
        this.instance = instance;
    }

    private Set<Integer> getPotentialCustomers(Route path) {
        Set<Integer> ret = new TreeSet<>(Comparator.comparingInt(instance::getDemand));
        for (int node : path.getNodes()) {
            ret.addAll(instance.getReverseNeighborhood(node));
        }
        return ret;
    }

    private List<Route> getPathsInBasis(List<Route> paths,
                                        RMPLinearSolution rmpSolution) {
        List<Route> sortedPaths = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (rmpSolution.getPrimalValue(i) > EPSILON) {
                sortedPaths.add(paths.get(i));
            }
        }
        sortedPaths.sort(Comparator.comparing(Route::getCost));
        return sortedPaths;
    }

    private int getTotalDemand(Route path) {
        return path.getCustomersServed().stream().map(instance::getDemand).reduce(Integer::sum).orElse(0);
    }

    private boolean canMerge(Route path1, Route path2, Route replacement) {
        if (replacement.getCost() < path1.getCost() + path2.getCost()) {
            Set<Integer> potentialCustomers = getPotentialCustomers(replacement);
            return potentialCustomers.containsAll(path1.getCustomersServed()) &&
                    potentialCustomers.containsAll(path2.getCustomersServed());
        }
        return false;
    }

    private Optional<Route> computeReplacement(Route path1, Route path2,
                                               List<Route> replacements) {
        if (getTotalDemand(path1) + getTotalDemand(path2) > instance.getCapacity()) {
            return Optional.empty();
        }
        for (Route replacement : replacements) {
            if (canMerge(path1, path2, replacement)) {
                Route ret = replacement.getCopyWithoutCustomers();
                ret.addCustomers(path1.getCustomersServed());
                ret.addCustomers(path2.getCustomersServed());
                return Optional.of(ret);
            }
        }
        return Optional.empty();
    }

    private List<Route> mergeHeuristic(List<Route> pathsToBeMerged, List<Route> replacements) {
        BitSet merged = new BitSet(pathsToBeMerged.size());
        for (int i = 0; i < pathsToBeMerged.size(); i++) {
            Route path1 = pathsToBeMerged.get(i);
            for (int j = 0; j < i; j++) {
                Route path2 = pathsToBeMerged.get(j);
                if (!merged.get(i) && !merged.get(j)) {
                    Optional<Route> replacement = computeReplacement(path1, path2, replacements);
                    if (replacement.isPresent()) {
                        pathsToBeMerged.add(replacement.get());
                        merged.set(i);
                        merged.set(j);
                    }
                }
            }
        }
        List<Route> ret = new ArrayList<>();
        for (int i = 0; i < pathsToBeMerged.size(); i++) {
            if (!merged.get(i)) {
                ret.add(pathsToBeMerged.get(i));
            }
        }
        return ret;
    }

    public List<Route> run(List<Route> paths, RMPLinearSolution rmpSolution) {
        List<Route> ret     = new ArrayList<>();
        BitSet      visited = new BitSet(instance.getNumberOfNodes());
        for (Route path : getPathsInBasis(paths, rmpSolution)) {
            if (visited.cardinality() >= instance.getNumberOfCustomers()) {
                break;
            }
            int           cumulativeDemand = 0;
            Route         currentPath      = path.getCopyWithoutCustomers();
            List<Integer> currentCustomers = new ArrayList<>();
            for (int customer : getPotentialCustomers(path)) {
                if (!visited.get(customer)) {
                    cumulativeDemand += instance.getDemand(customer);
                    if (cumulativeDemand > instance.getCapacity()) {
                        currentPath.addCustomers(currentCustomers);
                        currentCustomers = new ArrayList<>();
                        ret.add(currentPath);
                        currentPath = path.getCopyWithoutCustomers();
                        cumulativeDemand = instance.getDemand(customer);
                    }
                    currentCustomers.add(customer);
                    visited.set(customer);
                }
            }
            if (!currentCustomers.isEmpty()) {
                currentPath.addCustomers(currentCustomers);
                ret.add(currentPath);
            }
        }
        ret.removeAll(paths);
        return mergeHeuristic(ret, paths);
    }
}
