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

package net.uncontended.precipice.core.test_utils;

import net.uncontended.precipice.core.PrecipiceFunction;
import net.uncontended.precipice.core.concurrent.PrecipicePromise;

import java.util.concurrent.CountDownLatch;

public class TestCallbacks {

    public static <T> PrecipiceFunction<T> completePromiseCallback(final PrecipicePromise<T> promiseToComplete) {
        return new PrecipiceFunction<T>() {
            @Override
            public void apply(T result) {
                promiseToComplete.complete(result);
            }
        };
    }

    public static <T> PrecipiceFunction<T> latchedCallback(final CountDownLatch latch) {
        return new PrecipiceFunction<T>() {
            @Override
            public void apply(T resultPromise) {
                latch.countDown();
            }
        };
    }

    public static <T> PrecipiceFunction<T> exceptionCallback(T type) {
        return new PrecipiceFunction<T>() {
            @Override
            public void apply(T exception) {
                throw new RuntimeException("Boom");
            }
        };
    }
}
