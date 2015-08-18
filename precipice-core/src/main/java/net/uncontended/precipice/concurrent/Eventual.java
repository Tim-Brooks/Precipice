/*
 * Copyright 2014 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Status;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Eventual<T> implements PrecipiceFuture<T>, PrecipicePromise<T> {
    private final PrecipicePromise<T> wrappedPromise;
    private volatile T result;
    private volatile Throwable throwable;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private final AtomicReference<PrecipiceFunction<T>> successCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<Throwable>> errorCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<Void>> timeoutCallback = new AtomicReference<>();
    private PrecipiceFunction<Void> internalSuccessCallback;
    private PrecipiceFunction<Void> internalErrorCallback;
    private PrecipiceFunction<Void> internalTimeoutCallback;

    public Eventual() {
        this(null);
    }

    public Eventual(PrecipicePromise<T> promise) {
        wrappedPromise = promise;
    }

    @Override
    public boolean complete(T result) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                this.result = result;
                if (internalSuccessCallback != null) {
                    internalSuccessCallback.apply(null);
                }
                latch.countDown();
                PrecipiceFunction<T> cb = successCallback.get();
                if (cb != null && successCallback.compareAndSet(cb, null)) {
                    cb.apply(result);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.complete(result);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                this.throwable = ex;
                if (internalErrorCallback != null) {
                    internalErrorCallback.apply(null);
                }
                latch.countDown();
                PrecipiceFunction<Throwable> cb = errorCallback.get();
                if (cb != null && errorCallback.compareAndSet(cb, null)) {
                    cb.apply(ex);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.completeExceptionally(ex);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeWithTimeout() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                if (internalTimeoutCallback != null) {
                    internalTimeoutCallback.apply(null);
                }
                latch.countDown();
                PrecipiceFunction<Void> cb = timeoutCallback.get();
                if (cb != null && timeoutCallback.compareAndSet(cb, null)) {
                    cb.apply(null);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.completeWithTimeout();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public PrecipiceFuture<T> future() {
        return this;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        if (result != null) {
            return result;
        } else if (isCancelled()) {
            throw new CancellationException();
        } else if (throwable != null) {
            throw new ExecutionException(throwable);
        } else {
            return null;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            if (result != null) {
                return result;
            } else if (isCancelled()) {
                throw new CancellationException();
            } else {
                throw new ExecutionException(throwable);
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public boolean isDone() {
        return status.get() != Status.PENDING;
    }

    @Override
    public boolean isCancelled() {
        return status.get() == Status.CANCELLED;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean isCancelled = status.compareAndSet(Status.PENDING, Status.CANCELLED);
        if (isCancelled) {
            latch.countDown();
        }
        return isCancelled;
    }

    @Override
    public void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public void await(long duration, TimeUnit unit) throws InterruptedException {
        latch.await(duration, unit);
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable error() {
        return throwable;
    }

    @Override
    public void onSuccess(PrecipiceFunction<T> fn) {
        if (status.get() == Status.SUCCESS) {
            fn.apply(result);
        } else {
            if (successCallback.compareAndSet(null, fn)
                    && status.get() == Status.SUCCESS
                    && successCallback.compareAndSet(fn, null)) {
                fn.apply(result);
            }
        }
    }

    @Override
    public void onError(PrecipiceFunction<Throwable> fn) {
        if (status.get() == Status.ERROR) {
            fn.apply(throwable);
        } else {
            if (errorCallback.compareAndSet(null, fn)
                    && status.get() == Status.ERROR
                    && errorCallback.compareAndSet(fn, null)) {
                fn.apply(throwable);
            }
        }
    }

    @Override
    public void onTimeout(PrecipiceFunction<Void> fn) {
        if (status.get() == Status.TIMEOUT) {
            fn.apply(null);
        } else {
            if (timeoutCallback.compareAndSet(null, fn)
                    && status.get() == Status.TIMEOUT
                    && timeoutCallback.compareAndSet(fn, null)) {
                fn.apply(null);
            }
        }
    }

    @Override
    public Status getStatus() {
        return status.get();
    }

    public void internalOnSuccess(PrecipiceFunction<Void> fn) {
        internalSuccessCallback = fn;
    }

    public void internalOnError(PrecipiceFunction<Void> fn) {
        internalErrorCallback = fn;
    }

    public void internalOnTimeout(PrecipiceFunction<Void> fn) {
        internalTimeoutCallback = fn;
    }
}
