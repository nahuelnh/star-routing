package algorithm.pricing;

import algorithm.RestrictedMasterProblem;
import commons.FeasiblePath;
import commons.Instance;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class LabelSettingPricing implements PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;

    public LabelSettingPricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    @Override
    public Solution solve(RestrictedMasterProblem.RMPSolution rmpSolution) {
        paths = new LabelSettingAlgorithm(instance, rmpSolution).run();
//        System.out.println(instance.getCapacity());
//       IntStream.rangeClosed(0, instance.getNumberOfCustomers()-1).mapToDouble(
//                rmpSolution::getCustomerDual).forEach(System.out::println);
//        System.out.println(paths);
        return new Solution(IloCplex.Status.Optimal, 0.0, this);
    }

    @Override
    public List<FeasiblePath> computePathsFromSolution() {
        return paths;
    }
}
