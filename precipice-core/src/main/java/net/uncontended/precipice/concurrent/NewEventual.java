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

import net.uncontended.precipice.PerformingContext;
import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Result;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class NewEventual<S extends Result, T> implements PrecipiceFuture<S, T>, PrecipicePromise<S, T>,
        PerformingContext {

    private final long startNanos;
    private final PrecipicePromise<S, T> wrappedPromise;
    private volatile T result;
    private volatile Throwable throwable;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<S> status = new AtomicReference<>(null);
    private final AtomicReference<PrecipiceFunction<S, T>> successCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<S, Throwable>> errorCallback = new AtomicReference<>();
    private PrecipiceFunction<S, PerformingContext> internalCallback;

    public NewEventual() {
        this(System.nanoTime());
    }

    public NewEventual(long startNanos) {
        this(startNanos, null);
    }

    public NewEventual(long startNanos, PrecipicePromise<S, T> promise) {
        this.startNanos = startNanos;
        wrappedPromise = promise;
    }

    @Override
    public boolean complete(S status, T result) {
        if (this.status.get() == null) {
            if (this.status.compareAndSet(null, status)) {
                this.result = result;
                if (internalCallback != null) {
                    // TODO: Maybe move this to be different method
                    internalCallback.apply(status, this);
                }
                latch.countDown();
                PrecipiceFunction<S, T> cb = successCallback.get();
                if (cb != null && successCallback.compareAndSet(cb, null)) {
                    cb.apply(status, result);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.complete(status, result);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(S status, Throwable ex) {
        if (this.status.get() == null) {
            if (this.status.compareAndSet(null, status)) {
                throwable = ex;
                if (internalCallback != null) {
                    internalCallback.apply(status, this);
                }
                latch.countDown();
                PrecipiceFunction<S, Throwable> cb = errorCallback.get();
                if (cb != null && errorCallback.compareAndSet(cb, null)) {
                    cb.apply(status, ex);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.completeExceptionally(status, ex);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public PrecipiceFuture<S, T> future() {
        return this;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        if (result != null) {
            return result;
        } else if (isCancelled()) {
            throw new CancellationException();
        } else {
            throw new ExecutionException(throwable);
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
        return status.get() != null;
    }

    @Override
    public boolean isCancelled() {
        return false;
//        return status.get() == Status.CANCELLED;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
//        boolean isCancelled = status.compareAndSet(Status.PENDING, Status.CANCELLED);
//        if (isCancelled) {
//            latch.countDown();
//        }
//        return isCancelled;
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
    public void onSuccess(PrecipiceFunction<S, T> fn) {
        // TODO: Decide whether it is okay to execute multiple callbacks.
        S localStatus = status.get();
        if (localStatus != null && !localStatus.isFailure()) {
            fn.apply(localStatus, result);
        } else {
            if (successCallback.compareAndSet(null, fn)) {
                S localStatus2 = status.get();
                if (localStatus2 != null && !localStatus2.isFailure() && successCallback.compareAndSet(fn, null)) {
                    fn.apply(localStatus2, result);
                }
            }

        }
    }

    @Override
    public void onError(PrecipiceFunction<S, Throwable> fn) {
        S localStatus = status.get();
        if (localStatus != null && localStatus.isFailure()) {
            fn.apply(localStatus, throwable);
        } else {
            if (errorCallback.compareAndSet(null, fn)) {
                S localStatus2 = status.get();
                if (localStatus2 != null && localStatus2.isFailure() && errorCallback.compareAndSet(fn, null)) {
                    fn.apply(localStatus2, throwable);
                }
            }
        }
    }

    @Override
    public S getStatus() {
        return status.get();
    }

    public void internalOnComplete(PrecipiceFunction<S, PerformingContext> fn) {
        internalCallback = fn;
    }

    public long startNanos() {
        return startNanos;
    }
}
