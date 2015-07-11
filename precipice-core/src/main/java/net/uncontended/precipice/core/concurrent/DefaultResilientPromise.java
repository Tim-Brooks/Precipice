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

public class DefaultResilientPromise<T> extends AbstractResilientPromise<T> {

    @Override
    public boolean deliverResult(T result) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                this.result = result;
                latch.countDown();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deliverError(Exception error) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                this.error = error;
                latch.countDown();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setTimedOut() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                latch.countDown();
                return true;
            }
        }
        return false;
    }

}
