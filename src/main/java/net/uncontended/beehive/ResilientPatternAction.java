package net.uncontended.beehive;

/**
 * An action that returns a result and may throw an exception. This class
 * exists to be submitted or performed on a {@link Pattern}. It is very
 * similar to the {@link ResilientAction}. The primary difference is that
 * the Pattern will pass a C context to the {@code run} method. The context
 * is the specific context for the {@link ServiceExecutor} this action
 * is being ran on.
 *
 * @param <T> the result returned by {@code run}
 * @param <C> the context passed to {@code run}
 */
public interface ResilientPatternAction<T, C> {

    T run(C context) throws Exception;
}
