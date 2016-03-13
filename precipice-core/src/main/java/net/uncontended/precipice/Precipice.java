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
 * A <code>Precipice</code> is a class that has an associated GuardRail. This interface
 * should be implemented by classes that combined the usage of a GuardRail with specialized
 * execution logic. The only method {@link #guardRail()} should return the GuardRail associated
 * with the implemented class. The {@link CallService} is an example of an implementation.
 */
public interface Precipice<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    /**
     * Return the GuardRail associated with this class.
     *
     * @return the GuardRail
     */
    GuardRail<Result, Rejected> guardRail();
}
