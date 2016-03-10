package net.uncontended.precipice;

/**
 * A read only view of the result of a computation.
 *
 * @param <Result> the type of the result for the computation
 * @param <V>      the type of the value resulting from the computation
 */
public interface ResultView<Result extends Failable, V> {
    /**
     * Return the value of a successful execution.
     *
     * @return the value
     */
    V getValue();

    /**
     * Return the exception that might have occurred during a failed execution.
     *
     * @return the exception
     */
    Throwable getError();

    /**
     * Return the result of the execution.
     *
     * @return the result
     */
    Result getResult();
}
