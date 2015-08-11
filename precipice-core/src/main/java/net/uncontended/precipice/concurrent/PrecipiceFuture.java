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

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Status;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface PrecipiceFuture<T> extends Future<T> {

    void onSuccess(PrecipiceFunction<T> fn);

    void onError(PrecipiceFunction<Throwable> fn);

    void onTimeout(PrecipiceFunction<Void> fn);

    void await() throws InterruptedException;

    void await(long duration, TimeUnit unit) throws InterruptedException;

    T result();

    Throwable error();

    Status getStatus();
}
