package commons;

import java.util.List;

public class Solution {
    private final List<ElementaryPath> paths;
    private final int cost;
    private final int numberOfVehicles;

    public Solution(List<ElementaryPath> paths) {
        this.paths = paths;
        this.cost = paths.stream().mapToInt(ElementaryPath::getCost).sum();
        this.numberOfVehicles = paths.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("commons.Solution{cost=").append(cost).append(", numberOfVehicles=").append(numberOfVehicles)
                .append(", paths=[");
        if (!paths.isEmpty()) {
            builder.append('\n');
        }
        for (ElementaryPath path : paths) {
            builder.append("\t");
            builder.append(path);
            builder.append('\n');
        }
        builder.append("]}");
        return builder.toString();
    }
}
