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

package net.uncontended.precipice.samples;


import net.uncontended.precipice.timeout.PrecipiceTimeoutException;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class Callables {
    public static Callable<Integer> success() {
        return new Success();
    }

    public static Callable<Integer> error() {
        return new Error();
    }

    public static Callable<Integer> timeout(CountDownLatch latch) {
        return new Timeout(latch);
    }

    public static Callable<Integer> timeoutException() {
        return new TimeoutException();
    }


    private static class Success implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            return 8 * 8;
        }
    }

    private static class Error implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            throw new RuntimeException("Action failed.");
        }
    }

    private static class TimeoutException implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            throw new PrecipiceTimeoutException();
        }
    }

    private static class Timeout implements Callable<Integer> {

        private final CountDownLatch latch;

        public Timeout(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Integer call() throws Exception {
            latch.await();
            return 1;
        }
    }
}
