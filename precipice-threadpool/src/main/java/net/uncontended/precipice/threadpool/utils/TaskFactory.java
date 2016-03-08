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

package net.uncontended.precipice.threadpool.utils;

import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.threadpool.CancellableTask;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class TaskFactory {

    private static final CancellableTask.ResultToStatus<TimeoutableResult, Object> resultToStatus = new Success();
    private static final CancellableTask.ThrowableToStatus<TimeoutableResult> throwableToStatus = new Error();

    public static <T> CancellableTask<TimeoutableResult, T>
    createTask(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise) {
        CancellableTask.ResultToStatus<TimeoutableResult, T> castedResultToStatus =
                (CancellableTask.ResultToStatus<TimeoutableResult, T>) resultToStatus;
        return new CancellableTask<>(castedResultToStatus, throwableToStatus, callable, promise);
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
