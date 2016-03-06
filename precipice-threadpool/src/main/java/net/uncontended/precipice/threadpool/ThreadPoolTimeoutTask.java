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
import net.uncontended.precipice.timeout.TimeoutTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadPoolTimeoutTask<T> implements Runnable, TimeoutTask {

    static final CancellableTask.ResultToStatus<TimeoutableResult, Object> resultToStatus = new Success();
    static final CancellableTask.ThrowableToStatus<TimeoutableResult> throwableToStatus = new Error();
    private CancellableTask<TimeoutableResult, T> cancellableTask;
    public final long nanosAbsoluteTimeout;
    public final long millisRelativeTimeout;

    public ThreadPoolTimeoutTask(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise,
                                 long millisRelativeTimeout, long nanosAbsoluteStart) {
        this(new CancellableTask<>((CancellableTask.ResultToStatus<TimeoutableResult, T>) resultToStatus,
                throwableToStatus, callable, promise), millisRelativeTimeout, nanosAbsoluteStart);
    }

    public ThreadPoolTimeoutTask(CancellableTask<TimeoutableResult, T> cancellableTask,
                                 long millisRelativeTimeout, long nanosAbsoluteStart) {
        this.millisRelativeTimeout = millisRelativeTimeout;
        nanosAbsoluteTimeout = TimeUnit.NANOSECONDS.convert(millisRelativeTimeout, TimeUnit.MILLISECONDS) + nanosAbsoluteStart;
        this.cancellableTask = cancellableTask;
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
        if (o instanceof ThreadPoolTimeoutTask) {
            return Long.compare(nanosAbsoluteTimeout, ((ThreadPoolTimeoutTask<T>) o).nanosAbsoluteTimeout);
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
