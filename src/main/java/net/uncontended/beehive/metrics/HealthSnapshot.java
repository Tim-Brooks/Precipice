package net.uncontended.beehive.metrics;

/**
 * Created by timbrooks on 6/11/15.
 */
public class HealthSnapshot {
    public final long total;
    public final long failures;
    public final long rejections;

    public HealthSnapshot(long total, long failures, long rejections) {
        this.total = total;
        this.failures = failures;
        this.rejections = rejections;
    }

    public double failurePercentage() {
        return failures / total;
    }

    public double rejectionPercentage() {
        return rejections / total;
    }
}
