package net.uncontended.precipice;

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.NoOpCircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.concurrent.ExecutorService;

/**
 * Created by timbrooks on 6/3/15.
 */
public class Services {

    public static final int MAX_CONCURRENCY_LEVEL = Integer.MAX_VALUE / 2;

    public static Service defaultService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, concurrencyLevel);
    }

    public static Service defaultServiceWithNoOpBreaker(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, concurrencyLevel, new NoOpCircuitBreaker());
    }

    public static Service defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, concurrencyLevel, metrics);
    }

    public static Service defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, concurrencyLevel, metrics, breaker);
    }

    public static Service defaultService(ExecutorService executor, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        return new DefaultService(executor, concurrencyLevel, metrics, breaker);
    }
}
