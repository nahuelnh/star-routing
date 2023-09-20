package commons;

import java.time.Duration;
import java.util.List;

public class Solution {
    private final List<FeasiblePath> paths;
    private final double objValue;
    private final int numberOfVehicles;
    private final Duration elapsedTime;
    public Solution(List<FeasiblePath> paths, Duration elapsedTime) {
        this.paths = paths;
        this.objValue = paths.stream().mapToInt(FeasiblePath::getCost).sum();
        this.numberOfVehicles = paths.size();
        this.elapsedTime = elapsedTime;
    }

    public Solution(double objValue, List<FeasiblePath> paths, Duration elapsedTime) {
        this.paths = paths;
        this.objValue = objValue;
        this.numberOfVehicles = paths.size();
        this.elapsedTime = elapsedTime;
    }

    public double getObjValue() {
        return objValue;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Solution{cost=").append(objValue).append(", numberOfVehicles=").append(numberOfVehicles)
                .append(", elapsedTime=").append(elapsedTime.toMillis()).append("ms, paths=[");
        if (!paths.isEmpty()) {
            builder.append('\n');
        }
        for (FeasiblePath path : paths) {
            builder.append("\t");
            builder.append(path);
            builder.append('\n');
        }
        builder.append("]}");
        return builder.toString();
    }
}
