package net.uncontended.beehive.concurrent;

import net.uncontended.beehive.Status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by timbrooks on 12/22/14.
 */
public abstract class AbstractResilientPromise<T> implements ResilientPromise<T> {
    protected volatile T result;
    volatile Throwable error;
    final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    final CountDownLatch latch = new CountDownLatch(1);
    ResilientPromise<T> wrappedPromise;

    @Override
    public void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public boolean await(long millis) throws InterruptedException {
        return latch.await(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public T awaitResult() throws InterruptedException {
        latch.await();
        return result;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public Status getStatus() {
        return status.get();
    }

    @Override
    public boolean isSuccessful() {
        return status.get() == Status.SUCCESS;
    }

    @Override
    public boolean isDone() {
        return status.get() != Status.PENDING;
    }

    @Override
    public boolean isError() {
        return status.get() == Status.ERROR;
    }

    @Override
    public boolean isTimedOut() {
        return status.get() == Status.TIMEOUT;
    }

    public void wrapPromise(ResilientPromise<T> promise) {
        wrappedPromise = promise;
    }

}
