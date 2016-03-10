package net.uncontended.precipice;

public interface Readable<S extends Failable, T> {
    /**
     * Return the result of the execution.
     *
     * @return the result
     */
    T getResult();

    /**
     * Return the exception that might have occurred from a failed execution.
     *
     * @return the exception
     */
    Throwable getError();

    /**
     * Return the status of the execution.
     *
     * @return the status
     */
    S getStatus();
}
