package net.uncontended.beehive;

import net.uncontended.beehive.circuit.CircuitBreaker;
import net.uncontended.beehive.concurrent.ResilientFuture;
import net.uncontended.beehive.concurrent.ResilientPromise;
import net.uncontended.beehive.metrics.ActionMetrics;

/**
 * A service that actions can be submitted to or performed on. A service
 * has associated metrics and a circuit breaker. If actions are failing or
 * the service is being overloaded with submissions, the service will
 * apply backpressure.
 */
public interface ServiceExecutor {
    long MAX_TIMEOUT_MILLIS = 1000 * 60 * 60 * 24;

    /**
     * Submits a {@link ResilientAction} that will be run asynchronously.
     * The result of the action will be delivered to the future returned
     * by this call. An attempt to cancel the action will be made if it
     * does not complete before the timeout.
     *
     * @param action        the action to submit
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientAction<T> action, long millisTimeout);

    /**
     * Submits a {@link ResilientAction} that will be run asynchronously similar to
     * {@link #submitAction(ResilientAction, long)}. However, at the completion of the task,
     * the provided callback will be executed. The callback will be run regardless of the result
     * of the action.
     *
     * @param action        the action to submit
     * @param callback      to run on action completion
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientAction<T> action, ResilientCallback<T> callback, long
            millisTimeout);

    /**
     * Submits a {@link ResilientAction} that will be run asynchronously
     * similar to {@link #submitAction(ResilientAction, long)}. However, at the
     * completion of the task, the result will be delivered to the promise provided.
     *
     * @param action        the action to submit
     * @param promise       a promise to which deliver the result
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientAction<T> action, ResilientPromise<T> promise, long
            millisTimeout);

    /**
     * Submits a {@link ResilientAction} that will be run asynchronously similar to
     * {@link #submitAction(ResilientAction, long)}. However, at the completion
     * of the task, the result will be delivered to the promise provided. And the provided
     * callback will be executed.
     *
     * @param action        the action to submit
     * @param promise       a promise to which deliver the result
     * @param callback      to run on action completion
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientAction<T> action, ResilientPromise<T> promise,
                                        ResilientCallback<T> callback, long millisTimeout);

    /**
     * Performs a {@link ResilientAction} that will be run synchronously on the calling
     * thread. However, at the completion of the task, the result will be delivered to
     * the promise provided. And the provided callback will be executed.
     * <p/>
     * If the ResilientAction throws a {@link ActionTimeoutException}, the result of
     * the action will be a timeout. Any other exception and the result of the action
     * will be an error.
     *
     * @param action the action to run
     * @param <T>    the type of the result of the action
     * @return a {@link ResilientPromise} representing result of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientPromise<T> performAction(ResilientAction<T> action);

    /**
     * Returns the {@link ActionMetrics} for this service.
     *
     * @return the metrics backing this service
     */
    ActionMetrics getActionMetrics();

    /**
     * Returns the {@link CircuitBreaker} for this service.
     *
     * @return the circuit breaker for this service
     */
    CircuitBreaker getCircuitBreaker();

    /**
     * Attempts to shutdown the service. Calls made to submitAction or performAction
     * after this call will throw a {@link RejectedActionException}. Implementations
     * may differ on if pending or executing actions are cancelled.
     */
    void shutdown();
}
