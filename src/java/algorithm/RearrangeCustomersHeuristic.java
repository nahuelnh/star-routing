package algorithm;

import commons.FeasiblePath;
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

    private Set<Integer> getPotentialCustomers(FeasiblePath path) {
        Set<Integer> ret = new TreeSet<>(Comparator.comparingInt(instance::getDemand));
        for (int node : path.getNodes()) {
            ret.addAll(instance.getReverseNeighborhood(node));
        }
        return ret;
    }

    private List<FeasiblePath> getPathsInBasis(List<FeasiblePath> paths,
                                               RestrictedMasterProblem.RMPSolution rmpSolution) {
        List<FeasiblePath> sortedPaths = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (rmpSolution.getPrimalValue(i) > EPSILON) {
                sortedPaths.add(paths.get(i));
            }
        }
        sortedPaths.sort(Comparator.comparing(FeasiblePath::getCost));
        return sortedPaths;
    }

    private int getTotalDemand(FeasiblePath path) {
        return path.getCustomersServed().stream().map(instance::getDemand).reduce(Integer::sum).orElse(0);
    }

    private boolean canMerge(FeasiblePath path1, FeasiblePath path2, FeasiblePath replacement) {
        if (replacement.getCost() < path1.getCost() + path2.getCost()) {
            Set<Integer> potentialCustomers = getPotentialCustomers(replacement);
            return potentialCustomers.containsAll(path1.getCustomersServed()) &&
                    potentialCustomers.containsAll(path2.getCustomersServed());
        }
        return false;
    }

    private Optional<FeasiblePath> computeReplacement(FeasiblePath path1, FeasiblePath path2,
                                                      List<FeasiblePath> replacements) {
        if (getTotalDemand(path1) + getTotalDemand(path2) > instance.getCapacity()) {
            return Optional.empty();
        }
        for (FeasiblePath replacement : replacements) {
            if (canMerge(path1, path2, replacement)) {
                FeasiblePath ret = replacement.getCopyWithoutCustomers();
                ret.addCustomers(path1.getCustomersServed());
                ret.addCustomers(path2.getCustomersServed());
                return Optional.of(ret);
            }
        }
        return Optional.empty();
    }

    private List<FeasiblePath> mergeHeuristic(List<FeasiblePath> pathsToBeMerged, List<FeasiblePath> replacements) {
        BitSet merged = new BitSet(pathsToBeMerged.size());
        for (int i = 0; i < pathsToBeMerged.size(); i++) {
            FeasiblePath path1 = pathsToBeMerged.get(i);
            for (int j = 0; j < i; j++) {
                FeasiblePath path2 = pathsToBeMerged.get(j);
                if (!merged.get(i) && !merged.get(j)) {
                    Optional<FeasiblePath> replacement = computeReplacement(path1, path2, replacements);
                    if (replacement.isPresent()) {
                        pathsToBeMerged.add(replacement.get());
                        merged.set(i);
                        merged.set(j);
                    }
                }
            }
        }
        List<FeasiblePath> ret = new ArrayList<>();
        for (int i = 0; i < pathsToBeMerged.size(); i++) {
            if (!merged.get(i)) {
                ret.add(pathsToBeMerged.get(i));
            }
        }
        return ret;
    }

    public List<FeasiblePath> run(List<FeasiblePath> paths, RestrictedMasterProblem.RMPSolution rmpSolution) {
        List<FeasiblePath> ret = new ArrayList<>();
        BitSet visited = new BitSet(instance.getNumberOfNodes());
        for (FeasiblePath path : getPathsInBasis(paths, rmpSolution)) {
            if (visited.cardinality() >= instance.getNumberOfCustomers()) {
                break;
            }
            int cumulativeDemand = 0;
            FeasiblePath currentPath = path.getCopyWithoutCustomers();
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
