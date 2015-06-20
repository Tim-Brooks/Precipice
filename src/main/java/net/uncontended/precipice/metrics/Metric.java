package net.uncontended.precipice.metrics;

import net.uncontended.precipice.Status;

/**
 * Created by timbrooks on 6/1/15.
 */
public enum Metric {
    SUCCESS(false),
    ERROR(false),
    TIMEOUT(false),
    CIRCUIT_OPEN(true),
    QUEUE_FULL(true),
    MAX_CONCURRENCY_LEVEL_EXCEEDED(true);

    private final boolean actionRejected;

    Metric(boolean actionRejected) {
        this.actionRejected = actionRejected;
    }

    public boolean actionRejected() {
        return actionRejected;
    }

    public static Metric statusToMetric(Status status) {
        switch (status) {
            case SUCCESS:
                return Metric.SUCCESS;
            case ERROR:
                return Metric.ERROR;
            case TIMEOUT:
                return Metric.TIMEOUT;
            default:
                throw new RuntimeException("Cannot convert Status to Metric: " + status);
        }
    }
}
