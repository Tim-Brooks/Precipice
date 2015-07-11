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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractResilientPromise<T> implements ResilientPromise<T> {
    protected volatile T result;
    volatile Exception error;
    final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    @Override
    public T awaitResult() throws InterruptedException {
        latch.await();
        return result;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public Exception getError() {
        return error;
    }

    @Override
    public Status getStatus() {
        return status.get();
    }

    @Override
    public boolean isSuccessful() {
        return status.get() == Status.SUCCESS;
    }

    @Override
    public boolean isDone() {
        return status.get() != Status.PENDING;
    }

    @Override
    public boolean isError() {
        return status.get() == Status.ERROR;
    }

    @Override
    public boolean isTimedOut() {
        return status.get() == Status.TIMEOUT;
    }

}
