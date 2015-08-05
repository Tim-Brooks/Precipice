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

import net.uncontended.precipice.core.PrecipiceFunction;
import net.uncontended.precipice.core.Status;

import java.util.concurrent.Future;

public interface PrecipiceFuture<T> extends Future<T> {

    void onSuccess(PrecipiceFunction<T> fn);

    void onError(PrecipiceFunction<Throwable> fn);

    void onTimeout(PrecipiceFunction<Void> fn);

    void await() throws InterruptedException;

    T result();

    Throwable error();

    Status getStatus();
}
