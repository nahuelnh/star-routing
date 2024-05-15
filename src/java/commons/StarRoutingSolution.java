package commons;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StarRoutingSolution {
    private final List<FeasiblePath> paths;
    private final double objValue;
    private final Duration elapsedTime;
    private final Status status;
    private Optional<Double> lowerBound;
    private Optional<Double> deterministicTime;
    private boolean isInteger;

    public StarRoutingSolution(Status status, double objValue, Duration elapsedTime, boolean isInteger) {
        this.status = status;
        this.paths = new ArrayList<>();
        this.objValue = objValue;
        this.elapsedTime = elapsedTime;
        this.lowerBound = Optional.empty();
        this.deterministicTime = Optional.empty();
        this.isInteger = isInteger;
    }

    public StarRoutingSolution(Status status, double objValue, List<FeasiblePath> paths, Duration elapsedTime) {
        this.status = status;
        this.paths = paths;
        this.objValue = objValue;
        this.elapsedTime = elapsedTime;
        this.lowerBound = Optional.empty();
        this.deterministicTime = Optional.empty();
        this.isInteger = true;
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
        builder.append("StarRoutingSolution{status=").append(status.toString()).append(", cost=").append(objValue).append(", elapsedTime=")
                .append(elapsedTime.toMillis()).append("ms, lowerBound=").append(lowerBound.map(String::valueOf).orElse("null")).append(", paths=[");
        if (!paths.isEmpty()) {
            builder.append('\n');
            for (FeasiblePath path : paths) {
                builder.append("\t");
                builder.append(path);
                builder.append('\n');
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    public boolean timedOut() {
        return Status.TIMEOUT.equals(status);
    }

    public boolean hasLowerBound() {
        return lowerBound.isPresent();
    }

    public double getLowerBound() {
        assert lowerBound.isPresent();
        return lowerBound.get();
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = Optional.of(lowerBound);
    }

    public boolean hasDeterministicTime() {
        return deterministicTime.isPresent();
    }

    public double getDeterministicTime() {
        assert deterministicTime.isPresent();
        return deterministicTime.get();
    }

    public void setDeterministicTime(double deterministicTime) {
        this.deterministicTime = Optional.of(deterministicTime);
    }

    public boolean isInteger() {
        return isInteger;
    }

    public boolean isFeasible() {
        return !(Status.INFEASIBLE.equals(status) || Status.UNKNOWN.equals(status));
    }

    public enum Status {
        OPTIMAL,
        TIMEOUT,
        FEASIBLE,
        INFEASIBLE,
        UNKNOWN
    }
}
