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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Eventual<T> implements Future<T> {
    private volatile T result;
    private Exception error;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            if (result != null) {
                return result;
            } else {
                throw new ExecutionException(error);
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public boolean isDone() {
        return status.get() != Status.PENDING;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancelling is not supported at this time");
    }
}
