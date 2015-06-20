package net.uncontended.precipice;

import net.uncontended.precipice.circuit.BreakerConfig;
import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by timbrooks on 6/15/15.
 */
public class LoadBalancers {

    public static <C> ComposedService<C> newRoundRobin(Map<Service, C> serviceToContext) {
        return new LoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> ComposedService<C> newRoundRobinWithSharedPool(List<C> contexts, String name, int poolSize, int
            concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        Map<Service, C> serviceToContext = new HashMap<>();
        for (C context : contexts) {
            BreakerConfig configBuilder = new BreakerConfigBuilder().build();
            ActionMetrics metrics = new DefaultActionMetrics();
            Service service = Services.defaultService(executor, concurrencyLevel, metrics,
                    new DefaultCircuitBreaker(metrics, configBuilder));
            serviceToContext.put(service, context);
        }
        return new LoadBalancer<>(serviceToContext, new RoundRobinStrategy(contexts.size()));
    }
}
