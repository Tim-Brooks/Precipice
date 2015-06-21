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

package net.uncontended.precipice.test_utils;

import net.uncontended.precipice.ResilientCallback;
import net.uncontended.precipice.concurrent.ResilientPromise;

import java.util.concurrent.CountDownLatch;

public class TestCallbacks {

    public static <T> ResilientCallback<T> completePromiseCallback(final ResilientPromise<ResilientPromise<T>>
                                                                           promiseToComplete) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> promise) {
                promiseToComplete.deliverResult(promise);
            }
        };
    }

    public static <T> ResilientCallback<T> latchedCallback(final CountDownLatch latch) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> resultPromise) {
                latch.countDown();
            }
        };
    }

    public static <T> ResilientCallback<T> exceptionCallback(T type) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> resultPromise) {
                throw new RuntimeException("Boom");
            }
        };
    }
}
