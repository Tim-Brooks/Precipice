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

import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;
import net.uncontended.precipice.timeout.TimeoutTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ThreadPoolTask<T> implements Runnable, TimeoutTask {

    private static final CancellableTask.ResultToStatus<TimeoutableResult, Object> resultToStatus = new Success();
    private static final CancellableTask.ThrowableToStatus<TimeoutableResult> throwableToStatus = new Error();
    private CancellableTask<TimeoutableResult, T> cancellableTask;
    public final long nanosAbsoluteTimeout;
    public final long millisRelativeTimeout;

    public ThreadPoolTask(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise, long millisRelativeTimeout,
                          long nanosAbsoluteStart) {
        CancellableTask.ResultToStatus<TimeoutableResult, T> castedResultToStatus =
                (CancellableTask.ResultToStatus<TimeoutableResult, T>) resultToStatus;
        this.cancellableTask = new CancellableTask<>(castedResultToStatus, throwableToStatus, callable, promise);
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
        cancellableTask.run();
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
        cancellableTask.cancel(TimeoutableResult.TIMEOUT, new PrecipiceTimeoutException());
    }

    @Override
    public long getMillisRelativeTimeout() {
        return millisRelativeTimeout;
    }

    private static class Success implements CancellableTask.ResultToStatus<TimeoutableResult, Object> {

        @Override
        public TimeoutableResult resultToStatus(Object result) {
            return TimeoutableResult.SUCCESS;
        }
    }

    private static class Error implements CancellableTask.ThrowableToStatus<TimeoutableResult> {

        @Override
        public TimeoutableResult throwableToStatus(Throwable throwable) {
            if (throwable instanceof TimeoutException) {
                return TimeoutableResult.TIMEOUT;
            } else {
                return TimeoutableResult.ERROR;
            }
        }
    }
}
