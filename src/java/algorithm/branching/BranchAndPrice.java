package algorithm.branching;

import algorithm.InitialSolutionHeuristic;
import algorithm.RMPIntegerSolution;
import algorithm.RMPLinearSolution;
import algorithm.RestrictedMasterProblem;
import algorithm.pricing.PricingProblem;
import algorithm.pricing.PricingSolution;
import commons.Route;
import commons.Instance;
import commons.StarRoutingSolution;
import commons.Stopwatch;
import commons.Utils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BranchAndPrice {

  private static final Double EPSILON = 0.01;

  private final RestrictedMasterProblem rmp;
  private final PricingProblem pricing;
  private final InitialSolutionHeuristic initialSolutionHeuristic;
  private final BranchingRuleManager branchingRuleManager;
  private int numberOfIterations;

  private double upperBound;

  public BranchAndPrice(
      Instance instance,
      RestrictedMasterProblem rmp,
      PricingProblem pricing,
      InitialSolutionHeuristic initialSolutionHeuristic) {
    this.rmp = rmp;
    this.pricing = pricing;
    this.initialSolutionHeuristic = initialSolutionHeuristic;
    this.branchingRuleManager = new BranchingRuleManager();
    this.upperBound = Double.MAX_VALUE;
    this.numberOfIterations = 0;
  }

  public StarRoutingSolution solve() {
    return solve(Utils.DEFAULT_TIMEOUT);
  }

  public StarRoutingSolution solve(Duration timeout) {
    Stopwatch stopwatch = new Stopwatch(timeout);
    List<Route> columnsToAdd = initialSolutionHeuristic.run();

    RMPIntegerSolution incumbent = null;
    ArrayDeque<Node> openNodes = new ArrayDeque<>();
    Node root = new Node(null, null);
    Node lastNode;
    Node currentNode = null;
    double rootRelaxationOptimal = Double.MAX_VALUE;
    upperBound = Double.MAX_VALUE;

    openNodes.add(root);

    // Process the nodes in DFS order
    while (!openNodes.isEmpty()) {
      lastNode = currentNode;
      currentNode = openNodes.removeFirst();

      updateSubproblems(lastNode, currentNode);

      // Generate columns & solve the linear relaxation
      double relaxationOptimal = Double.MAX_VALUE;
      RMPLinearSolution rmpSolution;
      while (true) {
        numberOfIterations++;
        rmp.addColumns(columnsToAdd);
        rmp.solveRelaxation(stopwatch.getRemainingTime());
        rmpSolution = rmp.getSolution();
        if (!rmpSolution.isFeasible() || stopwatch.timedOut()) {
          break;
        }
        relaxationOptimal = Math.min(relaxationOptimal, rmpSolution.getObjectiveValue());

        PricingSolution pricingSolution = pricing.solve(rmpSolution, stopwatch.getRemainingTime());
        if (!pricingSolution.isFeasible() || stopwatch.timedOut()) {
          break;
        }
        columnsToAdd = pricingSolution.getNegativeReducedCostPaths();
        if (columnsToAdd.isEmpty()) {
          break;
        }
      }

      // Mark as solved
      assert Math.abs(rmpSolution.getObjectiveValue() - relaxationOptimal) < EPSILON;
      if (!stopwatch.timedOut()) {
        currentNode.markSolved(rmpSolution.isFeasible() ? relaxationOptimal : Double.MAX_VALUE);
      }

      // If current == root
      if (currentNode.getParent() == null) {
        rootRelaxationOptimal = relaxationOptimal;
      }

      // Process node if the node is feasible and the relaxation is better than the incumbent
      if (rmpSolution.isFeasible() && Math.ceil(rmpSolution.getObjectiveValue()) < upperBound) {
        rmp.solveInteger(); // Assume this is fast
        RMPIntegerSolution rmpIntegerSolution = rmp.getIntegerSolution();

        // Node has an integer solution!
        if (rmpSolution.isInteger()) {
          upperBound = rmpSolution.getObjectiveValue();
          incumbent = rmpIntegerSolution;
          assert Math.abs(rmpIntegerSolution.getObjectiveValue() - rmpSolution.getObjectiveValue())
              < EPSILON;
        } else {
          // Even though the node is not integer, rmpIntegerSolution may be a good enough feasible
          // solution
          if (rmpIntegerSolution.getObjectiveValue() < upperBound) {
            upperBound = rmpIntegerSolution.getObjectiveValue();
            incumbent = rmpIntegerSolution;
          }

          // Set children to be processed
          for (Branch branch : branchingRuleManager.applyBranchingRules(rmpSolution)) {
            Node child = new Node(currentNode, branch);
            openNodes.addFirst(child);
            currentNode.addChild(child);
          }
        }
      }
    }

    // Finish
    double objectiveValue = root.isSolved() ? root.getObjectiveValue() : rootRelaxationOptimal;
    objectiveValue = incumbent == null ? objectiveValue : incumbent.getObjectiveValue();
    return buildSolution(stopwatch, objectiveValue, root.getLowerBound(), incumbent);
  }

  private void updateSubproblems(Node last, Node current) {
    if (last == null) {
      return;
    }
    List<Node> fromLast = last.pathToRoot();
    List<Node> fromCurrent = current.pathToRoot();
    int i = 0;
    while (!fromCurrent.contains(fromLast.get(i))) {
      rmp.removeBranch(fromLast.get(i).getBranch());
      pricing.removeBranch(fromLast.get(i).getBranch());
      ++i;
    }
    int j = fromCurrent.indexOf(fromLast.get(i)) - 1;
    while (j >= 0) {
      rmp.addBranch(fromCurrent.get(j).getBranch());
      pricing.addBranch(fromCurrent.get(j).getBranch());
      --j;
    }
  }

  private StarRoutingSolution buildSolution(
      Stopwatch stopwatch, double objectiveValue, double lowerBound, RMPIntegerSolution incumbent) {

    // Timed out in the first CG iteration, no information at all
    if (stopwatch.timedOut() && objectiveValue == Double.MAX_VALUE) {
      return new StarRoutingSolution(
          StarRoutingSolution.Status.UNKNOWN, objectiveValue, stopwatch.getElapsedTime(), false);
    }

    // If no integer solution was found
    if (incumbent == null) {
      return new StarRoutingSolution(
          StarRoutingSolution.Status.UNKNOWN, objectiveValue, stopwatch.getElapsedTime(), false);
    }

    // If the solution timed out
    if (stopwatch.timedOut()) {
      StarRoutingSolution solution =
          new StarRoutingSolution(
              StarRoutingSolution.Status.TIMEOUT,
              incumbent.getObjectiveValue(),
              incumbent.getUsedPaths(),
              stopwatch.getElapsedTime());
      solution.setLowerBound(lowerBound);
      return solution;
    }

    // If the solution is infeasible
    if (!incumbent.isFeasible()) {
      return new StarRoutingSolution(
          StarRoutingSolution.Status.INFEASIBLE, objectiveValue, stopwatch.getElapsedTime(), false);
    }

    // If the solution is feasible and integer
    StarRoutingSolution solution =
        new StarRoutingSolution(
            StarRoutingSolution.Status.OPTIMAL,
            incumbent.getObjectiveValue(),
            incumbent.getUsedPaths(),
            stopwatch.getElapsedTime());
    solution.setLowerBound(lowerBound);
    return solution;
  }

  public int getNumberOfIterations() {
    return numberOfIterations;
  }

  private static class Node {
    private final Node parent;
    private final Branch branch;
    private Optional<Double> objectiveValue;
    private final List<Node> children;

    public Node(Node parent, Branch branch) {
      this.parent = parent;
      this.branch = branch;
      this.objectiveValue = Optional.empty();
      this.children = new ArrayList<>();
    }

    public void addChild(Node child) {
      this.children.add(child);
    }

    public Node getParent() {
      return parent;
    }

    public double getLowerBound() {
      if (!isSolved()) {
        return Double.MAX_VALUE;
      }
      double currentNodeValue = objectiveValue.orElseThrow();
      double minValueOfChildren = Double.MAX_VALUE;
      if (children.isEmpty()) {
        return currentNodeValue;
      }
      for (Node child : this.children) {
        if (!child.isSolved()) {
          return currentNodeValue;
        }
        minValueOfChildren = Math.min(minValueOfChildren, child.getLowerBound());
      }
      assert minValueOfChildren >= currentNodeValue;
      return minValueOfChildren;
    }

    public void markSolved(double objectiveValue) {
      this.objectiveValue = Optional.of(objectiveValue);
    }

    public boolean isSolved() {
      return objectiveValue.isPresent();
    }

    public Double getObjectiveValue() {
      if (objectiveValue.isEmpty()) {
        throw new IllegalStateException();
      }
      return objectiveValue.get();
    }

    public List<Node> pathToRoot() {
      List<Node> ret = new ArrayList<>();
      Node current = this;
      while (current != null) {
        ret.add(current);
        current = current.parent;
      }
      return ret;
    }

    public Branch getBranch() {
      return branch;
    }
  }
}
