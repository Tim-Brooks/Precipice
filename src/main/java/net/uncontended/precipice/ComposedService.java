package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.concurrent.ResilientPromise;

/**
 * A group of services that actions can be run on. Different implementations can
 * have different strategies for how to actions are distributed across the services.
 * This class receives {@link ResilientPatternAction} opposed to {@link ResilientAction}.
 * <p/>
 * <p/> The {@link ResilientPatternAction} {@code run} method is passed a context
 * specific to the service on which it is run.
 *
 * @param <C> the context passed to an pattern action
 */
public interface ComposedService<C> {
    /**
     * Submits a {@link ResilientPatternAction} that will be run asynchronously.
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
    <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, long millisTimeout);

    /**
     * Submits a {@link ResilientPatternAction} that will be run asynchronously similar to
     * {@link #submitAction(ResilientPatternAction, long)}. However, at the completion of the
     * task, the provided callback will be executed. The callback will be run regardless of
     * the result of the action.
     *
     * @param action        the action to submit
     * @param callback      to run on action completion
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientCallback<T> callback, long millisTimeout);

    /**
     * Submits a {@link ResilientPatternAction} that will be run asynchronously similar to
     * {@link #submitAction(ResilientPatternAction, long)}. However, at the completion of
     * the task, the result will be delivered to the promise provided.
     *
     * @param action        the action to submit
     * @param promise       a promise to which deliver the result
     * @param millisTimeout milliseconds before the action times out
     * @param <T>           the type of the result of the action
     * @return a {@link ResilientFuture} representing pending completion of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientPromise<T> promise, long
            millisTimeout);

    /**
     * Submits a {@link ResilientPatternAction} that will be run asynchronously similar to
     * {@link #submitAction(ResilientPatternAction, long)}. However, at the completion
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
    <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                        ResilientCallback<T> callback, long millisTimeout);

    /**
     * Performs a {@link ResilientPatternAction} that will be run synchronously on the
     * calling thread. However, at the completion of the task, the result will be delivered
     * to the promise provided. And the provided callback will be executed.
     * <p/>
     * <p/>
     * If the ResilientPatternAction throws a {@link ActionTimeoutException}, the result
     * of the action will be a timeout. Any other exception and the result of the action
     * will be an error.
     *
     * @param action the action to run
     * @param <T>    the type of the result of the action
     * @return a {@link ResilientPromise} representing result of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> ResilientPromise<T> performAction(ResilientPatternAction<T, C> action);

    /**
     * Attempts to shutdown the service. Calls made to submitAction or performAction
     * after this call will throw a {@link RejectedActionException}. Implementations
     * may differ on if pending or executing actions are cancelled.
     */
    void shutdown();
}
