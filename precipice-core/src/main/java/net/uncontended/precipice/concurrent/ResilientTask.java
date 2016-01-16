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

import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ResilientTask<T> implements Runnable, Delayed {

    public final long nanosAbsoluteTimeout;
    public final long nanosAbsoluteStart;
    public final long millisRelativeTimeout;
    private final PrecipicePromise<Status, T> promise;
    private final ResilientAction<T> action;
    private volatile Thread runner;

    public ResilientTask(ResilientAction<T> action, PrecipicePromise<Status, T> promise, long millisRelativeTimeout,
                         long nanosAbsoluteStart) {
        this.action = action;
        this.promise = promise;
        this.millisRelativeTimeout = millisRelativeTimeout;
        this.nanosAbsoluteStart = nanosAbsoluteStart;
        if (millisRelativeTimeout == TimeoutService.NO_TIMEOUT) {
            nanosAbsoluteTimeout = 0;
        } else {
            nanosAbsoluteTimeout = TimeUnit.NANOSECONDS.convert(millisRelativeTimeout, TimeUnit.MILLISECONDS)
                    + nanosAbsoluteStart;
        }
    }

    @Override
    public void run() {
        try {
            if (!promise.future().isDone()) {
                runner = Thread.currentThread();
                T result = action.run();
                safeSetSuccess(result);
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (ActionTimeoutException e) {
            safeSetTimedOut(e);
        } catch (Throwable e) {
            safeSetErred(e);
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(nanosAbsoluteTimeout - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ResilientTask) {
            return Long.compare(nanosAbsoluteTimeout, ((ResilientTask<T>) o).nanosAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
    }

    public void setTimedOut() {
        if (!promise.future().isDone()) {
            safeSetTimedOut(new ActionTimeoutException());
            if (runner != null) {
                runner.interrupt();
            }
        }
    }

    private void safeSetSuccess(T result) {
        try {
            promise.complete(Status.SUCCESS, result);
        } catch (Throwable t) {
        }
    }

    private void safeSetErred(Throwable e) {
        try {
            promise.completeExceptionally(Status.ERROR, e);
        } catch (Throwable t) {
        }
    }

    private void safeSetTimedOut(ActionTimeoutException e) {
        try {
            promise.completeExceptionally(Status.TIMEOUT, e);
        } catch (Throwable t) {
        }
    }

}
