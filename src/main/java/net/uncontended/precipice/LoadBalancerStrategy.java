package net.uncontended.precipice;

/**
 * Created by timbrooks on 6/11/15.
 */
public interface LoadBalancerStrategy {

    int nextExecutorIndex();
}
