package net.uncontended.precipice;

import java.util.Map;

/**
 * Created by timbrooks on 6/15/15.
 */
public class LoadBalancers {

    public static <C> ComposedService<C> newRoundRobin(Map<Service, C> executorToContext) {
        return new LoadBalancer<>(executorToContext, new RoundRobinStrategy(executorToContext.size()));
    }
}
