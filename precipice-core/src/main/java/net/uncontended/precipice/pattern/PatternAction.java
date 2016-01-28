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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.AsyncService;

/**
 * An action that returns a result and may throw an exception. It is very
 * similar to the {@link ResilientAction}. The primary difference is that
 * the AsyncPattern will pass a C context to the {@code run} method. The context
 * is the specific context for the {@link AsyncService} this action
 * is being ran on.
 *
 * @param <T> the result returned by {@code run}
 * @param <C> the context passed to {@code run}
 */
public interface PatternAction<T, C> {

    T run(C context) throws Exception;
}
