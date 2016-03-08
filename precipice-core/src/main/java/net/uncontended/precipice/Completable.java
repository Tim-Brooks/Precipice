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

package net.uncontended.precipice;

/**
 * A context that can be completed with the result of a computation.
 * The context can only be completed once. This holds true for both an
 * successes and exceptions. So if it is completed with an exception,
 * it cannot be completed successfully.
 *
 * @param <S> the type of the status for this promise
 * @param <T> the type of the result for this promise
 */
public interface Completable<S extends Failable, T> {

    /**
     * Completes this context successfully with the result. A boolean will be
     * returned indicating if it was completed successfully.
     *
     * @param status of the computation
     * @param result of the computation
     * @return if the context was competed successfully
     */
    boolean complete(S status, T result);

    /**
     * Completes this context with an exception. A boolean will be returned
     * indicating if it was completed successfully.
     *
     * @param status    of the computation
     * @param exception of the computation
     * @return if the context was competed successfully
     */
    boolean completeExceptionally(S status, Throwable exception);
}
