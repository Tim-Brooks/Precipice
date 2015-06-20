package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.Status;

/**
 * Created by timbrooks on 12/19/14.
 */
public interface ResilientPromise<T> {
    boolean deliverResult(T result);

    boolean deliverError(Throwable error);

    void await() throws InterruptedException;

    boolean await(long millis) throws InterruptedException;

    T awaitResult() throws InterruptedException;

    T getResult();

    Throwable getError();

    Status getStatus();

    boolean setTimedOut();

    boolean isSuccessful();

    boolean isDone();

    boolean isError();

    boolean isTimedOut();

}
