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

import net.uncontended.precipice.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PrecipiceExecutors {

    public static ExecutorService threadPoolExecutor(String name, int poolSize, int concurrencyLevel) {
        if (concurrencyLevel > Service.MAX_CONCURRENCY_LEVEL) {
            throw new IllegalArgumentException("Concurrency Level \"" + concurrencyLevel + "\" is greater than the " +
                    "allowed maximum: " + Service.MAX_CONCURRENCY_LEVEL + ".");
        }
        return new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.DAYS,
                new ArrayBlockingQueue<Runnable>(concurrencyLevel * 2), new ServiceThreadFactory(name));
    }
}
