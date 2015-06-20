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

import java.util.concurrent.Callable;

/**
 * An action that returns a result and may throw an exception. This class
 * exists to be submitted or performed on a {@link Service}. Implementers
 * must only implement the run() method.
 *
 *  <p/>This class is very similar in behavior to {@link Callable}.
 *
 * @param <T> the result returned by {@code run}
 */
public interface ResilientAction<T> {

    T run() throws Exception;
}
