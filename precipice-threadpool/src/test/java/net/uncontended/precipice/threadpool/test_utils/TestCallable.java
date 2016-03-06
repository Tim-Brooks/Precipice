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

package net.uncontended.precipice.threadpool.test_utils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class TestCallable {

    public static Callable<String> success() {
        return success("Success");
    }

    public static <T> Callable<T> success(final T result) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                return result;
            }
        };
    }

    public static Callable<String> blocked(final CountDownLatch latch) {
        return new Callable<String>() {

            @Override
            public String call() throws Exception {
                latch.await();
                return "Success";
            }
        };
    }

    public static Callable<String> erred(final Exception exception) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw exception;
            }
        };
    }
}
