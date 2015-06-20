package net.uncontended.beehive.circuit;

/**
 * Created by timbrooks on 11/5/14.
 */
public interface CircuitBreaker {

    boolean isOpen();

    boolean allowAction();

    void informBreakerOfResult(boolean successful);

    void setBreakerConfig(BreakerConfig breakerConfig);

    BreakerConfig getBreakerConfig();

    void forceOpen();

    void forceClosed();
}
