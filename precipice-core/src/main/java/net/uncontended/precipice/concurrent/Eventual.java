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

import net.uncontended.precipice.*;
import net.uncontended.precipice.Readable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Eventual<S extends Failable, T> implements PrecipiceFuture<S, T>, PrecipicePromise<S, T>, ExecutionContext {

    private final long permitCount;
    private final long startNanos;
    private final Completable<S, T> wrappedPromise;
    private volatile T result;
    private volatile Throwable throwable;
    private volatile Cancellable cancellable;
    private volatile boolean isCancelled = false;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<S> status = new AtomicReference<>(null);
    private final AtomicReference<PrecipiceFunction<S, T>> successCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<S, Throwable>> errorCallback = new AtomicReference<>();
    private PrecipiceFunction<S, ExecutionContext> internalCallback;

    public Eventual() {
        this(0L);
    }

    public Eventual(long permitCount) {
        this(permitCount, System.nanoTime());
    }

    public Eventual(long permitCount, long startNanos) {
        this(permitCount, startNanos, null);
    }

    public Eventual(Completable<S, T> completable) {
        this(0L, System.nanoTime(), completable);
    }

    public Eventual(long permitCount, long startNanos, Completable<S, T> completable) {
        this.permitCount = permitCount;
        this.startNanos = startNanos;
        wrappedPromise = completable;
    }

    @Override
    public boolean complete(S status, T result) {
        if (this.status.get() == null) {
            if (this.status.compareAndSet(null, status)) {
                this.result = result;
                executeInternalCallback(status);
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
    public boolean completeExceptionally(S status, Throwable exception) {
        if (this.status.get() == null) {
            if (this.status.compareAndSet(null, status)) {
                throwable = exception;
                executeInternalCallback(status);
                latch.countDown();
                PrecipiceFunction<S, Throwable> cb = errorCallback.get();
                if (cb != null && errorCallback.compareAndSet(cb, null)) {
                    cb.apply(status, exception);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.completeExceptionally(status, exception);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Readable<S, T> readable() {
        return this;
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
        return isCancelled;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning && cancellable != null && !isDone()) {
            isCancelled = true;
            cancellable.cancel();
            return true;
        } else {
            return false;
        }
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
    public T getResult() {
        return result;
    }

    @Override
    public Throwable getError() {
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

    @Override
    public long startNanos() {
        return startNanos;
    }

    @Override
    public long permitCount() {
        return permitCount;
    }

    public void setCancellable(Cancellable cancellable) {
        this.cancellable = cancellable;
    }

    public void internalOnComplete(PrecipiceFunction<S, ExecutionContext> fn) {
        internalCallback = fn;
    }

    private void executeInternalCallback(S status) {
        if (internalCallback != null) {
            internalCallback.apply(status, this);
        }
    }
}
