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

package net.uncontended.precipice.core.concurrent;

import net.uncontended.precipice.core.Status;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class wrapping the result of an action that will be completed at some
 * point in the future.
 *
 * @param <T> the result returned by the action
 */
public class ResilientFuture<T> implements Future {

    public final ResilientPromise<T> promise;

    public ResilientFuture(ResilientPromise<T> promise) {
        this.promise = promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        promise.await();
        if (promise.isError()) {
            throw new ExecutionException(promise.getError());
        }

        return promise.getResult();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return promise.isDone();
    }

    /**
     * This will always throw UnsupportedOperationException. Cancelling is not supported.
     *
     * @param mayInterruptIfRunning flag
     * @throws UnsupportedOperationException
     * @return boolean
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancellation is not supported.");
    }


    /**
     * This will always return false since cancelling is not supported.
     *
     * @return boolean indicating if future is cancelled.
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns the {@link Status} of the future.
     *
     * @return status of the future
     */
    public Status getStatus() {
        return promise.getStatus();
    }
}
