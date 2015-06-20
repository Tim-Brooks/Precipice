package net.uncontended.precipice;

/**
 * Created by timbrooks on 1/15/15.
 */
public enum RejectionReason {
    CIRCUIT_OPEN,
    QUEUE_FULL,
    MAX_CONCURRENCY_LEVEL_EXCEEDED,
    ALL_SERVICES_REJECTED,
    SERVICE_SHUTDOWN
}
