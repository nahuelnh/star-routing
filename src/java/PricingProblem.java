import java.util.ArrayList;
import java.util.List;

public class PricingProblem {

    private final Instance instance;
    private final MasterProblem.Solution solution;

    PricingProblem(Instance instance, MasterProblem.Solution solution) {
        this.instance = instance;
        this.solution = solution;
    }

    List<Route> solve() {
        return new ArrayList<>();
    }

}
