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

import java.util.concurrent.TimeUnit;

public interface ResilientPromise<T> {
    /**
     * Deliver the result to this promise. If the promise has already been completed,
     * this method will return false.
     *
     * @param result the result of the promise
     * @return a boolean indicating if the result was successfully set.
     */
    boolean deliverResult(T result);

    /**
     * Deliver an error to this promise. If the promise has already been completed,
     * this method will return false.
     *
     * @param error the error to deliver
     * @return a boolean indicating if the result was successfully set.
     */
    boolean deliverError(Throwable error);

    /**
     * Block on this promise being completed.
     *
     * @throws InterruptedException
     */
    void await() throws InterruptedException;

    boolean await(long timePeriod, TimeUnit unit) throws InterruptedException;

    /**
     * Block on this promise being completed and return the result. If promise is
     * not successfully completed, this will return null.
     *
     * @return T the result of the promise.
     * @throws InterruptedException
     */
    T awaitResult() throws InterruptedException;

    T getResult();

    Throwable getError();

    Status getStatus();

    boolean setTimedOut();

    boolean isSuccessful();

    boolean isDone();

    boolean isError();

    boolean isTimedOut();

}
