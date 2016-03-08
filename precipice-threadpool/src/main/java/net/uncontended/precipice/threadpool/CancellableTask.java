/*
 * Copyright 2016 Timothy Brooks
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

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.Cancellable;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.result.TimeoutableResult;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class CancellableTask<Status extends Enum<Status> & Failable, T> implements Runnable, Cancellable {

    private static final int PENDING = 0;
    private static final int DONE = 1;
    private static final int INTERRUPTING = 2;

    private final PrecipicePromise<Status, T> promise;
    private final ResultToStatus<Status, T> resultToStatus;
    private final ThrowableToStatus<Status> throwableToStatus;
    private final Callable<T> callable;
    private final AtomicInteger state = new AtomicInteger(PENDING);
    private volatile Thread runner;

    public CancellableTask(ResultToStatus<Status, T> resultToStatus, ThrowableToStatus<Status> throwableToStatus,
                           Callable<T> callable, PrecipicePromise<Status, T> promise) {
        this.resultToStatus = resultToStatus;
        this.throwableToStatus = throwableToStatus;
        this.callable = callable;
        this.promise = promise;
    }

    @Override
    public void run() {
        try {
            int state = this.state.get();
            if (state == PENDING && !promise.future().isDone()) {
                runner = Thread.currentThread();
                T result = callable.call();
                safeSetSuccess(result);
            } else if (state == INTERRUPTING) {
                waitForInterruption();
            }
        } catch (InterruptedException e) {
        } catch (Throwable e) {
            safeSetErred(e);
        } finally {
            Thread.interrupted();
        }
    }

    @Override
    public void cancel() {
        cancel(null, null);
    }

    public void cancel(Status cancelledStatus, Exception exception) {
        if (state.get() == PENDING) {
            safeCancel(cancelledStatus, exception);
        }
    }

    private void safeSetSuccess(T result) {
        try {
            if (state.get() == PENDING && state.compareAndSet(PENDING, DONE)) {
                promise.complete(resultToStatus.resultToStatus(result), result);
                return;
            }
            if (state.get() == INTERRUPTING) {
                waitForInterruption();
            }
        } catch (Throwable t) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
        }
    }

    private void safeSetErred(Throwable e) {
        try {
            if (state.get() == PENDING && state.compareAndSet(PENDING, DONE)) {
                promise.completeExceptionally(throwableToStatus.throwableToStatus(e), e);
                return;
            }
            if (state.get() == INTERRUPTING) {
                waitForInterruption();
            }
        } catch (Throwable t) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
        }
    }

    private void safeCancel(Status status, Exception e) {
        try {
            if (state.compareAndSet(PENDING, INTERRUPTING)) {
                if (runner != null) {
                    runner.interrupt();
                }
                if (status != null) {
                    promise.completeExceptionally(status, e);
                }
                state.set(DONE);
            }
        } catch (Throwable t) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
        }
    }

    private void waitForInterruption() {
        while (state.get() == INTERRUPTING) {
            Thread.yield();
        }
    }

    @FunctionalInterface
    public interface ResultToStatus<Status extends Enum<Status> & Failable, Result> {
        Status resultToStatus(Result result);
    }

    @FunctionalInterface
    public interface ThrowableToStatus<Status extends Enum<Status> & Failable> {
        Status throwableToStatus(Throwable throwable);
    }
}
