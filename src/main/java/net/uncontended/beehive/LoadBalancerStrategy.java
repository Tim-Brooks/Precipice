package net.uncontended.beehive;

/**
 * Created by timbrooks on 6/11/15.
 */
public interface LoadBalancerStrategy {

    int nextExecutorIndex();
}
