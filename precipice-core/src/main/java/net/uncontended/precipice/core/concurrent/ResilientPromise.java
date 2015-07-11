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

import java.util.concurrent.TimeUnit;

/**
 * A class wrapping the result of an action that will be completed at some point in
 * the future. It is similar to {@link ResilientFuture}. However, unlike a future, a
 * promise can be written to. A promise can only completed once. Once it is completed,
 * all further attempts to update the status should fail.
 *
 * @param <T> the result returned by the action
 */
public interface ResilientPromise<T> {
    /**
     * Deliver the result to this promise. If the promise has already been completed,
     * this method will return false.
     *
     * @param result the result of the promise
     * @return a boolean indicating if the result was successfully delivered
     */
    boolean deliverResult(T result);

    /**
     * Deliver an error to this promise. If the promise has already been completed,
     * this method will return false.
     *
     * @param error the error to deliver
     * @return a boolean indicating if the error was successfully delivered
     */
    boolean deliverError(Exception error);

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
     * @return T the result of the promise
     * @throws InterruptedException
     */
    T awaitResult() throws InterruptedException;

    /**
     * Return any result that has been delivered. If promise is not successfully completed
     * or is still pending, this will return null.
     *
     * @return T the result of the promise
     */
    T getResult();

    /**
     * Return any error that has been delivered. If action did not error or is still pending,
     * this will return null.
     *
     * @return Exception the error of the promise
     */
    Exception getError();

    /**
     * Return the status of the promise.
     *
     * @return Status the status of the promise
     */
    Status getStatus();

    /**
     * Set the promise {@link Status} to timeout. If the promise has already been completed,
     * this method will return false.
     *
     * @return a boolean indicating if the status was successfully set to timeout
     */
    boolean setTimedOut();

    /**
     * Indicate if the {@link Status} of the promise is successful.
     *
     * @return boolean indicating if the status is successful
     */
    boolean isSuccessful();

    /**
     * Indicate if the {@link Status} of the promise is not pending.
     *
     * @return boolean indicating if the promise is not pending
     */
    boolean isDone();

    /**
     * Indicate if the {@link Status} of the promise is error.
     *
     * @return boolean indicating if the status is error
     */
    boolean isError();

    /**
     * Indicate if the {@link Status} of the promise is timeout.
     *
     * @return boolean indicating if the status is timeout
     */
    boolean isTimedOut();

}
