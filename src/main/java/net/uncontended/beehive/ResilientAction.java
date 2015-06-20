package net.uncontended.beehive;

import java.util.concurrent.Callable;

/**
 * An action that returns a result and may throw an exception. This class
 * exists to be submitted or performed on a {@link Service}. Implementers
 * must only implement the run() method.
 *
 *  <p/>This class is very similar in behavior to {@link Callable}.
 *
 * @param <T> the result returned by {@code run}
 */
public interface ResilientAction<T> {

    T run() throws Exception;
}
