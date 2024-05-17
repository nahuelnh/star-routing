package commons;

import java.util.ArrayList;
import java.util.List;

/**
 * Weighted directed graph
 */
public class Graph {

    private final List<List<Integer>> adjacencyList;
    private final List<Edge> edges;
    private final Edge[][] adjacencyMatrix;
    private final int size;

    public Graph(int size) {
        this.size = size;
        this.adjacencyList = new ArrayList<>(size);
        this.adjacencyMatrix = new Edge[size][size];
        this.edges = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int i, int j, int weight) {
        //        assert start < size && end < size;
        adjacencyList.get(i).add(j);
        Edge e = new Edge(i, j, weight);
        edges.add(e);
        adjacencyMatrix[i][j] = e;
    }

    public void removeEdge(int i, int j) {
        adjacencyMatrix[i][j] = null;
        edges.removeIf(e -> e.getStart() == i && e.getEnd() == j);
        adjacencyList.get(i).removeIf(node -> node == j);
    }

    public List<Integer> getAdjacentNodes(int node) {
        return adjacencyList.get(node);
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public int getSize() {
        return size;
    }

    public boolean containsEdge(int i, int j) {
        return adjacencyMatrix[i][j] != null;
    }

    public Edge getEdge(int i, int j) {
        return adjacencyMatrix[i][j];
    }

    public static class Edge {

        private final int start;
        private final int end;
        private final int weight;

        public Edge(int start, int end, int weight) {
            this.start = start;
            this.end = end;
            this.weight = weight;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getWeight() {
            return weight;
        }
    }

}
