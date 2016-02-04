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

import net.uncontended.precipice.concurrent.LongSemaphore;
import net.uncontended.precipice.threadpool.ThreadPoolService;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.concurrent.ExecutorService;

public final class Services {

    private Services() {}

    public static ThreadPoolService submissionService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        ControllerProperties<Status> controllerProperties = new ControllerProperties<>(Status.class);
        controllerProperties.semaphore(new LongSemaphore(concurrencyLevel));
        return new ThreadPoolService(executor, new Controller<>(name, controllerProperties));
    }

    public static ThreadPoolService submissionService(String name, int poolSize, ControllerProperties<Status> properties) {
        long concurrencyLevel = properties.semaphore().maxConcurrencyLevel();
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new ThreadPoolService(executor, new Controller<>(name, properties));
    }

    public static CallService runService(String name, int concurrencyLevel) {
        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
        properties.semaphore(new LongSemaphore(concurrencyLevel));
        return new CallService(new Controller<>(name, properties));
    }

    public static CallService runService(String name, ControllerProperties<Status> properties) {
        return new CallService(new Controller<>(name, properties));
    }
}
