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

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.ReadableView;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A context containing the result of an asynchronous computation.
 *
 * @param <Result> the type of the result for this future
 * @param <V> the type of the value for this future
 */
public interface PrecipiceFuture<Result extends Failable, V> extends Future<V>, ReadableView<Result, V> {

    /**
     * Attaches a callback to be executed if the future is completed successfully.
     * The function will be passed the result of the future and the value.
     * <p/>
     * This method only is guaranteed to be safe if it is called once. Specific implementations
     * may provide stronger guarantees.
     *
     * @param fn function to be executed
     */
    void onSuccess(PrecipiceFunction<Result, V> fn);

    /**
     * Attaches a callback to be executed if the future is not completed successfully.
     * The function will be passed the result of the future and any exception that occurred
     * during execution.
     * <p/>
     * This method only is guaranteed to be safe if it is called once. Specific implementations
     * may provide stronger guarantees.
     *
     * @param fn function to be executed
     */
    void onError(PrecipiceFunction<Result, Throwable> fn);

    /**
     * Block until the completion of the future.
     *
     * @throws InterruptedException
     */
    void await() throws InterruptedException;

    /**
     * Block until the completion of the future or until the time duration is exceeded.
     *
     * @param duration the maximum duration to wait
     * @param unit     the unit of the duration argument
     * @throws InterruptedException
     */
    void await(long duration, TimeUnit unit) throws InterruptedException;

}
