package commons;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {

    private final Instant start;
    private final Duration timeout;

    public Stopwatch(Duration timeout) {
        this.start = Instant.now();
        this.timeout = timeout;
    }

    public Duration getElapsedTime() {
        return Utils.getElapsedTime(start);
    }

    public Duration getRemainingTime() {
        return Utils.getRemainingTime(start, timeout);
    }

    public boolean timedOut() {
        return getRemainingTime().isNegative();
    }
}
