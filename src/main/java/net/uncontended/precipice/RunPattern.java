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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.ResilientPromise;

public interface RunPattern<C> extends Pattern {

    /**
     * Performs a {@link ResilientPatternAction} that will be run synchronously on the
     * calling thread. However, at the completion of the task, the result will be delivered
     * to the promise provided. And the provided callback will be executed.
     * <p/>
     * <p/>
     * If the ResilientPatternAction throws a {@link ActionTimeoutException}, the result
     * of the action will be a timeout. Any other exception and the result of the action
     * will be an error.
     *
     * @param action the action to run
     * @param <T>    the type of the result of the action
     * @return a {@link ResilientPromise} representing result of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> T performAction(ResilientPatternAction<T, C> action) throws Exception;
}
