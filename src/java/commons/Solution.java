package commons;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class Solution {
    private final List<FeasiblePath> paths;
    private final double objValue;
    private final Duration elapsedTime;
    private final Status status;
    private final Optional<Double> lowerBound;

    public Solution(Status status, double objValue, List<FeasiblePath> paths, Duration elapsedTime) {
        this.status = status;
        this.paths = paths;
        this.objValue = objValue;
        this.elapsedTime = elapsedTime;
        this.lowerBound = Optional.empty();
    }

    public Solution(Status status, double objValue, List<FeasiblePath> paths, Duration elapsedTime, double lowerBound) {
        this.status = status;
        this.paths = paths;
        this.objValue = objValue;
        this.elapsedTime = elapsedTime;
        this.lowerBound = Optional.of(lowerBound);
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
        builder.append("Solution{cost=").append(objValue).append(", elapsedTime=").append(elapsedTime.toMillis())
                .append("ms, paths=[");
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

    public boolean timedOut() {
        return Status.TIMEOUT.equals(status);
    }

    public Optional<Double> getLowerBound() {
        return lowerBound;
    }

    public enum Status {
        FINISHED,
        TIMEOUT;
    }
}
