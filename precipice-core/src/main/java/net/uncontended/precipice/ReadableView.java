package net.uncontended.precipice;

public interface ReadableView<Result extends Failable, T> {
    /**
     * Return the value of the execution.
     *
     * @return the value
     */
    T getValue();

    /**
     * Return the exception that might have occurred from a failed execution.
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
