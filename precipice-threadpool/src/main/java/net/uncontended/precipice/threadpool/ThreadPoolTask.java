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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;
import net.uncontended.precipice.timeout.TimeoutTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadPoolTask<T> implements Runnable, TimeoutTask {

    public final long nanosAbsoluteTimeout;
    public final long millisRelativeTimeout;
    private final PrecipicePromise<Status, T> promise;
    private final Callable<T> callable;
    private final AtomicInteger state = new AtomicInteger(PENDING);
    private static final int PENDING = 0;
    private static final int DONE = 1;
    private static final int INTERRUPTING = 2;
    private volatile Thread runner;

    public ThreadPoolTask(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisRelativeTimeout,
                          long nanosAbsoluteStart) {
        this.callable = callable;
        this.promise = promise;
        this.millisRelativeTimeout = millisRelativeTimeout;
        if (millisRelativeTimeout == TimeoutService.NO_TIMEOUT) {
            nanosAbsoluteTimeout = 0;
        } else {
            nanosAbsoluteTimeout = TimeUnit.NANOSECONDS.convert(millisRelativeTimeout, TimeUnit.MILLISECONDS)
                    + nanosAbsoluteStart;
        }
    }

    public boolean canTimeout() {
        return millisRelativeTimeout != TimeoutService.NO_TIMEOUT;
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
        } catch (PrecipiceTimeoutException e) {
            safeSetTimedOut(e);
        } catch (Throwable e) {
            safeSetErred(e);
        } finally {
            Thread.interrupted();
        }
    }

    private void waitForInterruption() {
        while (state.get() == INTERRUPTING) {
            Thread.yield();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(nanosAbsoluteTimeout - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ThreadPoolTask) {
            return Long.compare(nanosAbsoluteTimeout, ((ThreadPoolTask<T>) o).nanosAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
    }

    @Override
    public void setTimedOut() {
        if (state.get() == PENDING) {
            safeSetTimedOut(new PrecipiceTimeoutException());
        }
    }

    @Override
    public long getMillisRelativeTimeout() {
        return millisRelativeTimeout;
    }

    private void safeSetSuccess(T result) {
        try {
            if (state.get() == PENDING && state.compareAndSet(PENDING, DONE)) {
                promise.complete(Status.SUCCESS, result);
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
                promise.completeExceptionally(Status.ERROR, e);
                return;
            }
            if (state.get() == INTERRUPTING) {
                waitForInterruption();
            }
        } catch (Throwable t) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
        }
    }

    private void safeSetTimedOut(PrecipiceTimeoutException e) {
        try {
            if (state.compareAndSet(PENDING, INTERRUPTING)) {
                if (runner != null) {
                    runner.interrupt();
                }
                promise.completeExceptionally(Status.TIMEOUT, e);
                state.set(DONE);
            }
        } catch (Throwable t) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
        }
    }
}
