package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.Status;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by timbrooks on 1/10/15.
 */
public class ResilientFuture<T> implements Future {

    public final ResilientPromise<T> promise;

    public ResilientFuture(ResilientPromise<T> promise) {
        this.promise = promise;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        promise.await();
        if (promise.isError()) {
            throw new ExecutionException(promise.getError());
        }

        return promise.getResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean await = promise.await(unit.convert(timeout, TimeUnit.MILLISECONDS));
        if (!await) {
            throw new TimeoutException();
        }
        if (promise.isError()) {
            throw new ExecutionException(promise.getError());
        }
        return promise.getResult();
    }

    @Override
    public boolean isDone() {
        return promise.isDone();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancellation not supported at this time");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public Status getStatus() {
        return promise.getStatus();
    }
}
