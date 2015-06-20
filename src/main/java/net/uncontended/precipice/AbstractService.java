package net.uncontended.precipice;

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;

/**
 * Created by timbrooks on 12/23/14.
 */
public abstract class AbstractService implements Service {
    final ActionMetrics actionMetrics;
    final CircuitBreaker circuitBreaker;

    public AbstractService(CircuitBreaker circuitBreaker, ActionMetrics actionMetrics) {
        this.circuitBreaker = circuitBreaker;
        this.actionMetrics = actionMetrics;
    }

    public ActionMetrics getActionMetrics() {
        return actionMetrics;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

}
