package algorithm.pricing;

import algorithm.RMPLinearSolution;
import algorithm.branching.BranchOnVisitFlow;
import commons.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

public class LabelSettingAlgorithm {

  private static final double EPSILON = Utils.DEFAULT_EPSILON;
  private static final int STOP_AFTER_N_SOLUTIONS = 100000;
  private static final boolean STOP_EARLY = true;
  private static final Comparator<Label> LABEL_COMPARATOR =
      Comparator.comparing(Label::demand).thenComparing(Label::cost);

  private final Instance instance;
  private final RMPLinearSolution rmpSolution;
  private final ESPPRCGraph graph;
  private final ESPPRCGraph reversedGraph;
  private final Map<Integer, Double> dualValues;
  private final Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer;
  private final boolean applyHeuristics;
  private final double alpha;
  private final LabelContainer[] fwNonDominatedLabels;
  private final LabelContainer[] bwNonDominatedLabels;

  private int labelsProcessed;

  public LabelSettingAlgorithm(
      Instance instance, RMPLinearSolution rmpSolution, boolean applyHeuristics) {
    this.instance = instance;
    this.rmpSolution = rmpSolution;
    this.labelsProcessed = 0;
    this.applyHeuristics = applyHeuristics;
    this.graph = new ESPPRCGraph(instance);
    this.reversedGraph = new ESPPRCGraph(instance, true);
    this.dualValues = computeDualVariables(instance, rmpSolution);
    this.alpha = computeCostFactor(graph);
    this.fwNonDominatedLabels = selectLabelDump(applyHeuristics, graph, instance);
    this.bwNonDominatedLabels = selectLabelDump(applyHeuristics, graph, instance);
    this.branchesIndexedByCustomer = getBranchesIndexedByCustomer(rmpSolution, instance);
  }

  public LabelSettingAlgorithm(Instance instance, RMPLinearSolution rmpSolution) {
    this(instance, rmpSolution, false);
  }

  private static Map<Integer, Double> computeDualVariables(
      Instance instance, RMPLinearSolution rmpSolution) {
    Map<Integer, Double> dualValues = new HashMap<>();
    for (int s = 0; s < instance.getNumberOfCustomers(); s++) {
      dualValues.put(instance.getCustomer(s), rmpSolution.getCustomerDual(s));
    }
    return dualValues;
  }

  private static Map<Integer, List<BranchOnVisitFlow>> getBranchesIndexedByCustomer(
      RMPLinearSolution rmpSolution, Instance instance) {
    Map<Integer, List<BranchOnVisitFlow>> branchesIndexedByCustomer = new HashMap<>();
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      branchesIndexedByCustomer.putIfAbsent(branch.getCustomer(), new ArrayList<>());
      branchesIndexedByCustomer.get(branch.getCustomer()).add(branch);
    }
    for (int customer : instance.getCustomers()) {
      branchesIndexedByCustomer.putIfAbsent(customer, List.of());
    }
    return branchesIndexedByCustomer;
  }

  private static LabelContainer[] selectLabelDump(
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

  public List<Route> run(Duration timeLimit) {
    bidirectionalSearch(new Stopwatch(timeLimit));
    return join();
  }

  private double getLittleFakeCost(Label label, int customer) {
    if (graph.containsEdge(customer, label.node())) {
      return graph.getEdge(customer, label.node()).getWeight() * alpha;
    }
    return 0.0;
  }

  private Label extendToCustomer(Label label, int customer, boolean forward) {
    double updatedCost = label.cost() - dualValues.get(customer);

    if (applyHeuristics) {
      updatedCost += getLittleFakeCost(label, customer);
    }

    // Subtract branching dual variables
    for (BranchOnVisitFlow branch : branchesIndexedByCustomer.get(customer)) {
      int start = forward ? branch.getEdge().getStart() : branch.getEdge().getEnd();
      int end = forward ? branch.getEdge().getEnd() : branch.getEdge().getStart();
      if (label.containsEdge(start, end)) {
        updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
      }
    }

    BitSet newVisitedCustomers = (BitSet) label.visitedCustomers().clone();
    newVisitedCustomers.set(customer);

    return new Label(
        label.demand() + instance.getDemand(customer),
        updatedCost,
        label.node(),
        (BitSet) label.visitedNodes().clone(),
        newVisitedCustomers,
        label);
  }

  private Label extendToNode(Label label, int nextNode, boolean forward) {
    int currentNode = label.node();
    double updatedCost = label.cost();
    if (forward) {
      updatedCost += graph.getEdge(currentNode, nextNode).getWeight();
    } else {
      updatedCost += reversedGraph.getEdge(currentNode, nextNode).getWeight();
    }

    // Subtract branching dual variables
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      int start = forward ? branch.getEdge().getStart() : branch.getEdge().getEnd();
      int end = forward ? branch.getEdge().getEnd() : branch.getEdge().getStart();
      if (label.isCustomerVisited(branch.getCustomer())
          && start == currentNode
          && end == nextNode) {
        updatedCost -= rmpSolution.getVisitFlowDuals().get(branch);
      }
    }

    BitSet newVisitedNodes = (BitSet) label.visitedNodes().clone();
    newVisitedNodes.set(nextNode);

    return new Label(
        label.demand(),
        updatedCost,
        nextNode,
        newVisitedNodes,
        (BitSet) label.visitedCustomers().clone(),
        label);
  }

  /**
   * @return True iff new label is infeasible or dominated
   */
  private boolean isCustomerUnreachable(Label label, int customer, boolean forward) {

    // New label has to be feasible
    if (label.parent().isCustomerVisited(customer)) {
      return true;
    }
    if (label.demand() > instance.getCapacity()) {
      return true;
    }

    // Labels can only be extended as long as their consumption
    // of the selected resource (in this case, capacity)
    // does not exceed half of the availability
    if (2 * label.parent().demand() >= instance.getCapacity()) {
      return true;
    }

    if (dualValues.get(customer) < EPSILON) {
      // Heuristic: if a customer provides no reduction in total cost,
      // can be pruned
      return true;
    }

    // Branching pruning rules
    for (BranchOnVisitFlow branch : branchesIndexedByCustomer.get(customer)) {
      int start = forward ? branch.getEdge().getStart() : branch.getEdge().getEnd();
      int end = forward ? branch.getEdge().getEnd() : branch.getEdge().getStart();
      if (branch.getBound() == 1 && label.forbidsEdge(start, end)) {
        return true;
      } else if (branch.getBound() == 0 && label.containsEdge(start, end)) {
        return true;
      }
    }

    // Check dominance
    if (forward && fwNonDominatedLabels[label.node()].dominates(label)) {
      return true;
    }
    if (!forward && bwNonDominatedLabels[label.node()].dominates(label)) {
      return true;
    }
    return false;
  }

  /**
   * @return True iff new label is infeasible or dominated
   */
  private boolean isNodeUnreachable(Label label, boolean forward) {

    // New label has to be feasible
    int currentNode = label.node();
    int previousNode = label.parent().node();
    if (label.parent().isNodeVisited(currentNode)) {
      return true;
    }
    if (label.demand() > instance.getCapacity()) {
      return true;
    }

    // Labels can only be extended as long as their consumption
    // of the selected resource (in this case, capacity)
    // does not exceed half of the availability
    if (2 * label.parent().demand() >= instance.getCapacity()) {
      return true;
    }

    // Branching pruning rules
    for (BranchOnVisitFlow branch : rmpSolution.getVisitFlowDuals().keySet()) {
      if (label.isCustomerVisited(branch.getCustomer())) {
        int start = forward ? branch.getEdge().getStart() : branch.getEdge().getEnd();
        int end = forward ? branch.getEdge().getEnd() : branch.getEdge().getStart();
        if (branch.getBound() == 1 && start != previousNode && end == currentNode) {
          return true;
        } else if (branch.getBound() == 1 && start == previousNode && end != currentNode) {
          return true;
        } else if (branch.getBound() == 0 && start == previousNode && end == currentNode) {
          return true;
        }
      }
    }

    // Check dominance
    if (forward && fwNonDominatedLabels[currentNode].dominates(label)) {
      return true;
    }
    if (!forward && bwNonDominatedLabels[currentNode].dominates(label)) {
      return true;
    }

    return false;
  }

  private double getInitialCost() {
    double initialCost = -rmpSolution.getVehiclesDual();
    for (double fleetSizeDual : rmpSolution.getFleetSizeDuals()) {
      initialCost -= fleetSizeDual;
    }
    return initialCost;
  }

  private void bidirectionalSearch(Stopwatch stopwatch) {
    List<Queue<Label>> fwOpenLabels = new ArrayList<>();
    List<Queue<Label>> bwOpenLabels = new ArrayList<>();

    for (int i = 0; i < graph.getSize(); i++) {
      fwOpenLabels.add(new PriorityQueue<>(LABEL_COMPARATOR));
      bwOpenLabels.add(new PriorityQueue<>(LABEL_COMPARATOR));
    }

    Label fwRoot = Label.getRootLabel(graph.getSource(), graph.getSize(), getInitialCost());
    Label bwRoot = Label.getRootLabel(graph.getSink(), graph.getSize(), 0);
    fwNonDominatedLabels[fwRoot.node()].addLabel(fwRoot);
    bwNonDominatedLabels[bwRoot.node()].addLabel(bwRoot);
    fwOpenLabels.get(fwRoot.node()).add(fwRoot);
    bwOpenLabels.get(bwRoot.node()).add(bwRoot);

    Queue<Integer> openNodes = new ArrayDeque<>();
    openNodes.add(fwRoot.node());
    openNodes.add(bwRoot.node());

    while (!openNodes.isEmpty()) {

      int currentNode = openNodes.remove();
      if (stopwatch.timedOut()) {
        return;
      }

      Set<Integer> discovered = forwardSearch(fwOpenLabels, currentNode);
      discovered.addAll(backwardSearch(bwOpenLabels, currentNode));

      for (int nextNode : discovered) {
        if (nextNode != currentNode && !openNodes.contains(nextNode)) {
          openNodes.add(nextNode);
        }
      }
    }
  }

  private Set<Integer> backwardSearch(List<Queue<Label>> bwOpenLabels, int currentNode) {
    Set<Integer> discovered = new HashSet<>();
    Queue<Label> currentOpenLabels = bwOpenLabels.get(currentNode);
    while (!currentOpenLabels.isEmpty()) {
      Label currentLabel = bwOpenLabels.get(currentNode).remove();
      labelsProcessed++;

      // Extend to customers
      for (int customer : reversedGraph.getReverseNeighborhood(currentNode)) {
        Label nextLabel = extendToCustomer(currentLabel, customer, false);
        if (!isCustomerUnreachable(nextLabel, customer, false)) {
          bwNonDominatedLabels[currentNode].addLabel(nextLabel);
          currentOpenLabels.add(nextLabel);
        }
      }

      // Extend to nodes
      for (int nextNode : reversedGraph.getAdjacentNodes(currentNode)) {
        Label nextLabel = extendToNode(currentLabel, nextNode, false);
        if (!isNodeUnreachable(nextLabel, false)) {
          bwNonDominatedLabels[nextNode].addLabel(nextLabel);
          bwOpenLabels.get(nextNode).add(nextLabel);
          discovered.add(nextNode);
        }
      }
    }
    return discovered;
  }

  private Set<Integer> forwardSearch(List<Queue<Label>> fwOpenLabels, int currentNode) {
    Set<Integer> discovered = new HashSet<>();
    Queue<Label> currentOpenLabels = fwOpenLabels.get(currentNode);
    while (!currentOpenLabels.isEmpty()) {
      Label currentLabel = currentOpenLabels.remove();
      labelsProcessed++;

      // Extend to customers
      for (int customer : graph.getReverseNeighborhood(currentNode)) {
        Label nextLabel = extendToCustomer(currentLabel, customer, true);
        if (!isCustomerUnreachable(nextLabel, customer, true)) {
          fwNonDominatedLabels[currentNode].addLabel(nextLabel);
          currentOpenLabels.add(nextLabel);
        }
      }

      // Extend to nodes
      for (int nextNode : graph.getAdjacentNodes(currentNode)) {
        Label nextLabel = extendToNode(currentLabel, nextNode, true);
        if (!isNodeUnreachable(nextLabel, true)) {
          fwNonDominatedLabels[nextNode].addLabel(nextLabel);
          fwOpenLabels.get(nextNode).add(nextLabel);
          discovered.add(nextNode);
        }
      }
    }

    return discovered;
  }

  /**
   * @param forward a label representing a forward state
   * @param backward a label representing a backward state
   * @return True iff merging forward and backward produces a feasible route
   */
  private boolean canMerge(Label forward, Label backward) {
    if (forward.demand() + backward.demand() > instance.getCapacity()) {
      return false;
    }
    if (forward.visitedCustomers().intersects(backward.visitedCustomers())) {
      return false;
    }
    BitSet visitedNodesButSelf = (BitSet) forward.visitedNodes().clone();
    visitedNodesButSelf.clear(forward.node());
    if (visitedNodesButSelf.intersects(backward.visitedNodes())) {
      return false;
    }

    BitSet customers = (BitSet) forward.visitedCustomers().clone();
    customers.or(backward.visitedCustomers());

    for (int customer : Utils.bitSetToIntSet(customers)) {
      for (BranchOnVisitFlow branch : branchesIndexedByCustomer.get(customer)) {
        int start = branch.getEdge().getStart();
        int end = branch.getEdge().getEnd();
        if (branch.getBound() == 1
            && !forward.containsEdge(start, end)
            && !backward.containsEdge(end, start)) {
          return false;
        } else if (branch.getBound() == 0
            && (forward.containsEdge(start, end) || backward.containsEdge(end, start))) {
          return false;
        }
      }
    }
    return true;
  }

  private Route merge(Label forward, Label backward) {
    Route path = new Route();

    // Add forward nodes
    List<Integer> fwNodes = forward.getNodesInOrder();
    for (int j = 1; j < fwNodes.size(); j++) {
      int lastNode = fwNodes.get(j - 1);
      int currentNode = fwNodes.get(j);
      path.addNode(
          graph.translateFromESPPRCNode(currentNode),
          graph.getEdge(lastNode, currentNode).getWeight());
    }

    // Add backward nodes
    List<Integer> bwNodes = backward.getNodesInOrder(true);
    for (int j = 1; j < bwNodes.size(); j++) {
      int lastNode = bwNodes.get(j - 1);
      int currentNode = bwNodes.get(j);
      path.addNode(
          graph.translateFromESPPRCNode(currentNode),
          graph.getEdge(lastNode, currentNode).getWeight());
    }

    // Add customers
    BitSet customers = (BitSet) forward.visitedCustomers().clone();
    customers.or(backward.visitedCustomers());
    path.addCustomers(Utils.bitSetToIntSet(customers));

    return path;
  }

  private List<Route> join() {
    Set<Route> ret = new HashSet<>();
    int solutionsCount = 0;
    for (int i = 0; i < graph.getSize(); i++) {
      double upperBound = -EPSILON;
      List<Label> fwLabels = fwNonDominatedLabels[i].getLabels();
      List<Label> bwLabels = bwNonDominatedLabels[i].getLabels();

      if (fwLabels.getFirst().cost() + bwLabels.getFirst().cost() < upperBound) {
        for (Label forward : fwLabels) {
          if (forward.cost() + bwLabels.getFirst().cost() < upperBound) {
            for (Label backward : bwLabels) {
              if (forward.cost() + backward.cost() < upperBound) {
                if (canMerge(forward, backward)) {
                  ret.add(merge(forward, backward));
                  upperBound = forward.cost() + backward.cost();
                  solutionsCount++;

                  if (STOP_EARLY && solutionsCount >= STOP_AFTER_N_SOLUTIONS) {
                    return new ArrayList<>(ret);
                  }
                }
              }
            }
          }
        }
      }
    }
    return new ArrayList<>(ret);
  }

  private Optional<Integer> selectBestNode(
      List<Queue<Label>> fwOpenLabels, List<Queue<Label>> bwOpenLabels) {
    Optional<Integer> fwBestNode =
        IntStream.range(0, graph.getSize())
            .mapToObj(fwOpenLabels::get)
            .filter(q -> !q.isEmpty())
            .map(Queue::peek)
            .min(LABEL_COMPARATOR)
            .map(Label::node);
    Optional<Integer> bwBestNode =
        IntStream.range(0, graph.getSize())
            .mapToObj(bwOpenLabels::get)
            .filter(q -> !q.isEmpty())
            .map(Queue::peek)
            .min(LABEL_COMPARATOR)
            .map(Label::node);
    return fwBestNode.isEmpty() ? bwBestNode : fwBestNode;
  }

  public int getLabelsProcessed() {
    return labelsProcessed;
  }
}
