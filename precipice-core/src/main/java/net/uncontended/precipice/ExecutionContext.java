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

/**
 * A context representing the execution of a task that was allowed by a guardrail.
 */
public interface ExecutionContext {

    /**
     * Returns the time that the guard rail approved the execution of this task.
     *
     * @return the start nano time
     */
    long startNanos();

    /**
     * Returns the number of guard rail permits that this task required.
     * 
     * @return number of permits
     */
    long permitCount();
}
