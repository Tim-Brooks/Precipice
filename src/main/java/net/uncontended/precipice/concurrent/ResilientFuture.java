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

import net.uncontended.precipice.Status;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResilientFuture<T> implements Future {

    public final ResilientPromise<T> promise;

    public ResilientFuture(ResilientPromise<T> promise) {
        this.promise = promise;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        promise.await();
        if (promise.isError()) {
            throw new ExecutionException(promise.getError());
        }

        return promise.getResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean await = promise.await(timeout, unit);
        if (!await) {
            throw new TimeoutException();
        }
        if (promise.isError()) {
            throw new ExecutionException(promise.getError());
        }
        return promise.getResult();
    }

    @Override
    public boolean isDone() {
        return promise.isDone();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancellation is not supported.");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public Status getStatus() {
        return promise.getStatus();
    }
}
