package net.uncontended.beehive;

import net.uncontended.beehive.circuit.CircuitBreaker;
import net.uncontended.beehive.circuit.NoOpCircuitBreaker;
import net.uncontended.beehive.metrics.ActionMetrics;
import net.uncontended.beehive.utils.ServiceThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by timbrooks on 6/3/15.
 */
public class Services {

    public static final int MAX_CONCURRENCY_LEVEL = Integer.MAX_VALUE / 2;

    public static Service defaultService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService service = createExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(service, concurrencyLevel);
    }

    public static Service defaultServiceWithNoOpBreaker(String name, int poolSize, int concurrencyLevel) {
        ExecutorService service = createExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(service, concurrencyLevel, new NoOpCircuitBreaker());
    }

    public static Service defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics) {
        ExecutorService service = createExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(service, concurrencyLevel, metrics);
    }

    public static Service defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        ExecutorService service = createExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(service, concurrencyLevel, metrics, breaker);
    }

    private static ExecutorService createExecutor(String name, int poolSize, int concurrencyLevel) {
        if (concurrencyLevel > MAX_CONCURRENCY_LEVEL) {
            throw new IllegalArgumentException("Concurrency Level \"" + concurrencyLevel + "\" is greater than the " +
                    "allowed maximum: " + MAX_CONCURRENCY_LEVEL + ".");
        }
        return new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.DAYS,
                new ArrayBlockingQueue<Runnable>(concurrencyLevel * 2), new ServiceThreadFactory(name));
    }
}
