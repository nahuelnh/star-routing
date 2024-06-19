package commons;

import java.util.*;

/**
 * Generic graph
 */
public class Graph<N extends Graph.Node> {

    private final Map<Integer, N> nodes;
    private final Map<Integer, Map<Integer, Edge<N>>> adjacencyMatrix;
    private final int size;

    public Graph() {
        this.nodes = new HashMap<>();
        this.adjacencyMatrix = new HashMap<>();
        this.size = 0;
    }

    public void addNode(N node) {
        nodes.put(node.getId(), node);
        adjacencyMatrix.put(node.getId(), new HashMap<>());
    }

    public void addEdge(int i, int j) {
        adjacencyMatrix.get(i).put(j, new Edge<>(nodes.get(i), nodes.get(j)));
    }

    public void addEdge(int i, int j, int weight) {
        adjacencyMatrix.get(i).put(j, new IntWeightedEdge<>(nodes.get(i), nodes.get(j), weight));
    }

    public void addEdge(int i, int j, double weight) {
        adjacencyMatrix.get(i).put(j, new ContinuousWeightedEdge<>(nodes.get(i), nodes.get(j), weight));
    }

    public void removeEdge(int i, int j) {
        adjacencyMatrix.get(i).remove(j);
    }

    public Collection<Edge<N>> getIncidentEdges(int node) {
        return adjacencyMatrix.get(node).values();
    }


    public int getSize() {
        return size;
    }

    public boolean containsEdge(int i, int j) {
        return adjacencyMatrix.get(i).containsKey(j);
    }

    public Edge<N> getEdge(int i, int j) {
        assert containsEdge(i, j);
        return adjacencyMatrix.get(i).get(j);
    }

    public Collection<N> getNodes() {
        return nodes.values();
    }

    public static class Node {

        private final int id;

        public Node(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static class Edge<N extends Node> {

        private final N start;
        private final N end;


        public Edge(N start, N end) {
            this.start = start;
            this.end = end;
        }

        public N getStart() {
            return start;
        }

        public N getEnd() {
            return end;
        }

    }

    public static class IntWeightedEdge<N extends Node> extends Edge<N> {

        private final int weight;

        public IntWeightedEdge(N start, N end, int weight) {
            super(start, end);
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }

    }

    public static class ContinuousWeightedEdge<N extends Node> extends Edge<N> {

        private final double weight;

        public ContinuousWeightedEdge(N start, N end, double weight) {
            super(start, end);
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

    }

}
