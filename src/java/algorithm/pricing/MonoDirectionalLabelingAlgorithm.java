package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnVisitFlow;
import commons.Instance;
import commons.Route;
import commons.Stopwatch;
import commons.Utils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MonoDirectionalLabelingAlgorithm {

  private static final double EPSILON = Utils.DEFAULT_EPSILON;
  private static final int STOP_AFTER_N_SOLUTIONS = 100000;
  private static final boolean STOP_EARLY = false;
  private static final Comparator<Label> LABEL_COMPARATOR =
      Comparator.comparing(Label::demand).thenComparing(Label::cost);

  private final Instance instance;
  private final RMPLinearSolution rmpSolution;
  private final ESPPRCGraph graph;

  private final Map<Integer, Double> dualValues;
  private final Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer;
  private final boolean applyHeuristics;
  private final double alpha;
  private final LabelContainer[] labelContainer;
  private int labelsProcessed;

  public MonoDirectionalLabelingAlgorithm(
      Instance instance, RMPLinearSolution rmpSolution, boolean applyHeuristics) {
    this.instance = instance;
    this.rmpSolution = rmpSolution;
    this.dualValues = new HashMap<>();
    this.labelsProcessed = 0;
    for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
      dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
    }
    this.graph = new ESPPRCGraph(instance);
    this.applyHeuristics = applyHeuristics;
    this.alpha = computeCostFactor(graph);
    this.labelContainer = selectLabelContainer(applyHeuristics, graph, instance);
    this.branchesIndexedByCustomer = getBranchesIndexedByCustomer(rmpSolution);
  }

  private static Map<Integer, List<BranchOnVisitFlow>> getBranchesIndexedByCustomer(
      RMPLinearSolution rmpSolution) {
    Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer = new HashMap<>();
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      branchesIndexedByCustomer.putIfAbsent(branch.getCustomer(), new ArrayList<>());
      branchesIndexedByCustomer.get(branch.getCustomer()).add(branch);
    }
    return branchesIndexedByCustomer;
  }

  private static LabelContainer[] selectLabelContainer(
      boolean applyHeuristics, ESPPRCGraph graph, Instance instance) {
    if (applyHeuristics) {
      RelaxedLabelContainer[] ret = new RelaxedLabelContainer[graph.getSize()];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = new RelaxedLabelContainer(instance.getCapacity() + 1);
      }
      return ret;
    } else {
      ExactLabelContainer[] ret = new ExactLabelContainer[graph.getSize()];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = new ExactLabelContainer();
      }
      return ret;
    }
  }

  private static double computeCostFactor(ESPPRCGraph graph) {
    int sum = 0;
    for (int i = 0; i < graph.getSize(); i++) {
      for (int j = 0; j < graph.getSize(); j++) {
        if (graph.containsEdge(i, j)) {
          sum += graph.getEdge(i, j).getWeight();
        }
      }
    }
    return 1.0 / sum;
  }

  private List<Route> getNegativeReducedCostPaths() {
    return labelContainer[graph.getSink()].getNegativeReducedCostLabels().stream()
        .map(this::translateToFeasiblePath)
        .toList();
  }

  private Route translateToFeasiblePath(Label label) {
    Route feasiblePath = new Route();
    List<Integer> fwNodes = label.getNodesInOrder();
    for (int j = 1; j < fwNodes.size(); j++) {
      int lastNode = fwNodes.get(j - 1);
      int currentNode = fwNodes.get(j);
      feasiblePath.addNode(
          graph.translateFromESPPRCNode(currentNode),
          graph.getEdge(lastNode, currentNode).getWeight());
    }
    feasiblePath.addCustomers(Utils.bitSetToIntSet(label.visitedCustomers()));
    return feasiblePath;
  }

  public List<Route> run(Duration timeLimit) {
    Stopwatch stopwatch = new Stopwatch(timeLimit);
    monoDirectionalBacktracking(stopwatch);
    return getNegativeReducedCostPaths();
  }

  private double getLittleFakeCost(Label label, int customer) {
    if (graph.containsEdge(customer, label.node())) {
      return graph.getEdge(customer, label.node()).getWeight() * alpha;
    }
    return 0.0;
  }

  private Label extendCustomer(Label label, int customer) {
    int updatedDemand = label.demand() + instance.getDemand(customer);
    double updatedCost = label.cost() - dualValues.get(customer);
    if (applyHeuristics) {
      updatedCost += getLittleFakeCost(label, customer);
    }
    BitSet updatedVisited = (BitSet) label.visitedCustomers().clone();
    updatedVisited.set(customer);
    // Subtract branching dual variables
    for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(customer, List.of())) {
      if (label.containsEdge(branch.getEdge().getStart(), branch.getEdge().getEnd())) {
        updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
      }
    }
    return new Label(
        updatedDemand,
        updatedCost,
        label.node(),
        (BitSet) label.visitedNodes().clone(),
        updatedVisited,
        label);
  }

  private Label extendNode(Label label, int nextNode) {
    double updatedCost = label.cost() + graph.getEdge(label.node(), nextNode).getWeight();
    BitSet updatedVisited = (BitSet) label.visitedNodes().clone();
    updatedVisited.set(nextNode);
    // Subtract branching dual variables
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      if (label.isCustomerVisited(branch.getCustomer())
          && branch.getEdge().getStart() == label.node()
          && branch.getEdge().getEnd() == nextNode) {
        updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
      }
    }
    return new Label(
        label.demand(),
        updatedCost,
        nextNode,
        updatedVisited,
        (BitSet) label.visitedCustomers().clone(),
        label);
  }

  private boolean isCustomerUnreachable(Label label, int customer, Label previousLabel) {
    if (previousLabel.isCustomerVisited(customer)) {
      return true;
    }
    if (label.demand() > instance.getCapacity()) {
      return true;
    }
    if (dualValues.get(customer) < EPSILON) {
      // Heuristic: if customer provides no reduction in total cost, can be pruned
      return true;
    }
    // Branching pruning rules
    for (BranchOnVisitFlow branch : branchesIndexedByCustomer.getOrDefault(customer, List.of())) {
      int start = branch.getEdge().getStart();
      int end = branch.getEdge().getEnd();
      if (branch.getBound() == 1 && label.forbidsEdge(start, end)) {
        return true;
      } else if (branch.getBound() == 0 && label.containsEdge(start, end)) {
        return true;
      }
    }
    return labelContainer[label.node()].dominates(label);
  }

  private boolean isNodeUnreachable(Label label, Label previousLabel) {
    // returns true if node is infeasible or dominated
    if (previousLabel.isNodeVisited(label.node())) {
      return true;
    }
    if (label.demand() > instance.getCapacity()) {
      return true;
    }
    // Branching pruning rules
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      if (label.isCustomerVisited(branch.getCustomer())) {
        int start = branch.getEdge().getStart();
        int end = branch.getEdge().getEnd();
        if (branch.getBound() == 1 && start != previousLabel.node() && end == label.node()) {
          return true;
        } else if (branch.getBound() == 1 && start == previousLabel.node() && end != label.node()) {
          return true;
        } else if (branch.getBound() == 0 && start == previousLabel.node() && end == label.node()) {
          return true;
        }
      }
    }

    return labelContainer[label.node()].dominates(label);
  }

  private double getInitialCost() {
    double initialCost = -rmpSolution.getVehiclesDual();
    for (double fleetSizeDual : rmpSolution.getFleetSizeDuals()) {
      initialCost -= fleetSizeDual;
    }
    return initialCost;
  }

  private void monoDirectionalBacktracking(Stopwatch stopwatch) {
    Label root = Label.getRootLabel(graph.getSource(), graph.getSize(), getInitialCost());
    PriorityQueue<Label> queue = new PriorityQueue<>(64, Comparator.comparingDouble(Label::cost));
    labelContainer[root.node()].addLabel(root);
    queue.add(root);
    while (!queue.isEmpty()) {
      labelsProcessed++;
      if (stopwatch.timedOut()) {
        return;
      }
      Label currentLabel = queue.remove();

      if (!labelContainer[currentLabel.node()].dominates(currentLabel)) {
        for (int customer : graph.getReverseNeighborhood(currentLabel.node())) {
          Label nextLabel = extendCustomer(currentLabel, customer);
          if (!isCustomerUnreachable(nextLabel, customer, currentLabel)) {
            labelContainer[nextLabel.node()].addLabel(nextLabel);
            queue.add(nextLabel);
          }
        }
        for (int nextNode : graph.getAdjacentNodes(currentLabel.node())) {
          Label nextLabel = extendNode(currentLabel, nextNode);
          if (!isNodeUnreachable(nextLabel, currentLabel)) {
            labelContainer[nextNode].addLabel(nextLabel);
            queue.add(nextLabel);
          }
        }
      }
    }
  }

  public int getLabelsProcessed() {
    return labelsProcessed;
  }
}
