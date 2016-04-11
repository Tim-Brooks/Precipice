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
 * A mechanism to provide back pressure for task execution.
 *
 * @param <Rejected> the type for rejection reasons
 */
public interface BackPressure<Rejected extends Enum<Rejected>> {

    /**
     * Acquires the request permits for task execution. If the execution is denied due
     * to back pressure, then a the rejected reason is returned. If the permits are
     * successfully acquired, then null is returned.
     *
     * @param number   of permits requested
     * @param nanoTime currentInterval nanosecond time
     * @return the reason for rejection if permit acquisition fails
     */
    Rejected acquirePermit(long number, long nanoTime);

    /**
     * Releases permits without considering the result of the execution.
     *
     * @param number   of permits to release
     * @param nanoTime currentInterval nanosecond time
     */
    void releasePermit(long number, long nanoTime);

    /**
     * Releases permits while considering the result of the execution. The result of the
     * execution may help inform the logic of the back pressure mechanism (depending on the
     * implementation).
     *
     * @param number   of permits to release
     * @param result   of the task execution
     * @param nanoTime currentInterval nanosecond time
     */
    void releasePermit(long number, Failable result, long nanoTime);

    /**
     * This method will register a guard rail with this back pressure mechanism. It is called
     * when when constructing a guard rail. This allows back pressure decisions to be made
     * based upon the object associated with guard rail.
     *
     * @param guardRail   the guard rail registered
     */
    <Result extends Enum<Result> & Failable> void registerGuardRail(GuardRail<Result, Rejected> guardRail);
}
