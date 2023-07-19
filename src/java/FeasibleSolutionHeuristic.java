import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeasibleSolutionHeuristic {

    private final Instance instance;

    public FeasibleSolutionHeuristic(Instance instance) {
        this.instance = instance;
    }

    public List<ElementaryPath> run() {
        List<ElementaryPath> ret = new ArrayList<>();

        ElementaryPath currentElementaryPath = ElementaryPath.emptyPath();
        int cumulativeDemand = 0;
        int lastNode = instance.getDepot();

        for (int currentNode : instance.getCustomers()) {
            cumulativeDemand += instance.getDemand(currentNode);
            if (cumulativeDemand > instance.getCapacity()) {
                currentElementaryPath.addNode(instance.getDepot(), new HashSet<>(),
                        instance.getEdgeWeight(lastNode, instance.getDepot()));
                ret.add(currentElementaryPath);
                currentElementaryPath = ElementaryPath.emptyPath();
                cumulativeDemand = 0;
                lastNode = instance.getDepot();
            }
            currentElementaryPath.addNode(currentNode, Set.of(currentNode),
                    instance.getEdgeWeight(lastNode, currentNode));
            lastNode = currentNode;
        }
        currentElementaryPath.addNode(instance.getDepot(), new HashSet<>(),
                instance.getEdgeWeight(lastNode, instance.getDepot()));
        ret.add(currentElementaryPath);


        // TODO revisar!!!
//        while (ret.size() < instance.getNumberOfVehicles()) {
//            ElementaryPath path = ElementaryPath.emptyPath();
//            int firstCustomer = instance.getCustomer(0);
//            path.addNode(firstCustomer, new HashSet<>(), instance.getEdgeWeight(instance.getDepot(), firstCustomer));
//            path.addNode(instance.getDepot(), new HashSet<>(),
//                    instance.getEdgeWeight(firstCustomer, instance.getDepot()));
//            ret.add(path);
//        }

        return ret;
    }
}
