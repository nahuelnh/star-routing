package algorithm.pricing;

import algorithm.RMPLinearSolution;
import commons.FeasiblePath;
import commons.Instance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PulsePricing extends PricingProblem {

    private final Instance instance;
    private List<FeasiblePath> paths;

    public PulsePricing(Instance instance) {
        this.instance = instance;
        this.paths = new ArrayList<>();
    }

    private double getObjValue(FeasiblePath path, Map<Integer, Double> dualValues, double vehiclesDual) {
        double sum = path.getCustomersServed().stream().map(dualValues::get).reduce(Double::sum).orElse(0.0);
        return path.getCost() - sum - vehiclesDual;
    }

    private double getMinObjValue(RMPLinearSolution rmpSolution) {
        Map<Integer, Double> dualValues = new HashMap<>();
        for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
            dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
        }
        return paths.stream().mapToDouble(path -> getObjValue(path, dualValues, rmpSolution.getVehiclesDual())).min()
                .orElse(0.0);
    }

    @Override
    public PricingSolution solve(RMPLinearSolution rmpSolution, Duration remainingTime) {
        performBranching();
        PulseAlgorithm pulseAlgorithm = new PulseAlgorithm(instance, rmpSolution);
        paths = pulseAlgorithm.run(remainingTime);
        return new PricingSolution(getMinObjValue(rmpSolution), paths, pulseAlgorithm.getPulsesPropagated(), true);
    }

    @Override
    public void forceExactSolution() {
    }

}
