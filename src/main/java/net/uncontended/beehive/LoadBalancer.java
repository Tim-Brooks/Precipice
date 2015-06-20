package net.uncontended.beehive;

import java.util.Map;

/**
 * Created by timbrooks on 6/15/15.
 */
public class LoadBalancer {

    public static <C> Pattern<C> roundRobin(Map<ServiceExecutor, C> executorToContext) {
        return new LoadBalancerPattern<>(new RoundRobinStrategy(executorToContext.size()), executorToContext);
    }
}
