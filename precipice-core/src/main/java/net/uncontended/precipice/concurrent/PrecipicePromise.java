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

import net.uncontended.precipice.Completable;
import net.uncontended.precipice.Failable;

/**
 * A context that can be completed with the result of an asynchronous computation.
 *
 * @param <S> the type of the status for this promise
 * @param <T> the type of the result for this promise
 */
public interface PrecipicePromise<S extends Failable, T> extends Completable<S,T> {

    /**
     * Returns a future containing the result of this promise
     *
     * @return a future
     */
    PrecipiceFuture<S, T> future();
}
