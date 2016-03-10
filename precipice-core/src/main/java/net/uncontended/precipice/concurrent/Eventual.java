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
import net.uncontended.precipice.ResultView;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Eventual<Result extends Failable, V> implements PrecipiceFuture<Result, V>, PrecipicePromise<Result, V>,
        ExecutionContext {

    private final long permitCount;
    private final long startNanos;
    private final Completable<Result, V> wrappedPromise;
    private volatile V value;
    private volatile Throwable throwable;
    private volatile Cancellable cancellable;
    private volatile boolean isCancelled = false;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Result> result = new AtomicReference<>(null);
    private final AtomicReference<PrecipiceFunction<Result, V>> successCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<Result, Throwable>> errorCallback = new AtomicReference<>();
    private PrecipiceFunction<Result, ExecutionContext> internalCallback;

    public Eventual() {
        this(0L);
    }

    public Eventual(long permitCount) {
        this(permitCount, System.nanoTime());
    }

    public Eventual(long permitCount, long startNanos) {
        this(permitCount, startNanos, null);
    }

    public Eventual(Completable<Result, V> completable) {
        this(0L, System.nanoTime(), completable);
    }

    public Eventual(long permitCount, long startNanos, Completable<Result, V> completable) {
        this.permitCount = permitCount;
        this.startNanos = startNanos;
        wrappedPromise = completable;
    }

    @Override
    public boolean complete(Result result, V value) {
        if (this.result.get() == null) {
            if (this.result.compareAndSet(null, result)) {
                this.value = value;
                executeInternalCallback(result);
                latch.countDown();
                PrecipiceFunction<Result, V> cb = successCallback.get();
                if (cb != null && successCallback.compareAndSet(cb, null)) {
                    cb.apply(result, value);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.complete(result, value);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(Result result, Throwable exception) {
        if (this.result.get() == null) {
            if (this.result.compareAndSet(null, result)) {
                throwable = exception;
                executeInternalCallback(result);
                latch.countDown();
                PrecipiceFunction<Result, Throwable> cb = errorCallback.get();
                if (cb != null && errorCallback.compareAndSet(cb, null)) {
                    cb.apply(result, exception);
                }
                if (wrappedPromise != null) {
                    wrappedPromise.completeExceptionally(result, exception);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ResultView<Result, V> resultView() {
        return this;
    }

    @Override
    public PrecipiceFuture<Result, V> future() {
        return this;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        latch.await();
        if (value != null) {
            return value;
        } else if (isCancelled()) {
            throw new CancellationException();
        } else {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            if (value != null) {
                return value;
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
        return result.get() != null;
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
    public V getValue() {
        return value;
    }

    @Override
    public Throwable getError() {
        return throwable;
    }

    @Override
    public void onSuccess(PrecipiceFunction<Result, V> fn) {
        // TODO: Decide whether it is okay to execute multiple callbacks.
        Result localResult = result.get();
        if (localResult != null && !localResult.isFailure()) {
            fn.apply(localResult, value);
        } else {
            if (successCallback.compareAndSet(null, fn)) {
                Result localResult2 = result.get();
                if (localResult2 != null && !localResult2.isFailure() && successCallback.compareAndSet(fn, null)) {
                    fn.apply(localResult2, value);
                }
            }

        }
    }

    @Override
    public void onError(PrecipiceFunction<Result, Throwable> fn) {
        Result localResult = result.get();
        if (localResult != null && localResult.isFailure()) {
            fn.apply(localResult, throwable);
        } else {
            if (errorCallback.compareAndSet(null, fn)) {
                Result localResult2 = result.get();
                if (localResult2 != null && localResult2.isFailure() && errorCallback.compareAndSet(fn, null)) {
                    fn.apply(localResult2, throwable);
                }
            }
        }
    }

    @Override
    public Result getResult() {
        return result.get();
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

    public void internalOnComplete(PrecipiceFunction<Result, ExecutionContext> fn) {
        internalCallback = fn;
    }

    private void executeInternalCallback(Result result) {
        if (internalCallback != null) {
            internalCallback.apply(result, this);
        }
    }
}
