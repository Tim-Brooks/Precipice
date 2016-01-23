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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.concurrent.ExecutorService;

public final class Services {

    private Services() {}

    public static AsyncService submissionService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);

        ControllerProperties<Status> controllerProperties = new ControllerProperties<>(Status.class);
        controllerProperties.semaphore(new LongSemaphore(concurrencyLevel));
        return new DefaultAsyncService(executor, new Controller<>(name, controllerProperties));
    }

    public static AsyncService submissionService(String name, int poolSize, ServiceProperties properties) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, properties.concurrencyLevel());

        ControllerProperties<Status> controllerProperties = new ControllerProperties<>(Status.class);
        controllerProperties.actionMetrics((ActionMetrics<Status>) properties.actionMetrics());
        controllerProperties.latencyMetrics((LatencyMetrics<Status>) properties.latencyMetrics());
        controllerProperties.semaphore(properties.semaphore());
        controllerProperties.circuitBreaker(properties.circuitBreaker());

        return new DefaultAsyncService(executor, new Controller<>(name, controllerProperties));
    }

    public static RunService runService(String name, int concurrencyLevel) {
        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
        properties.semaphore(new LongSemaphore(concurrencyLevel));
        return new DefaultRunService(new Controller<>(name, properties));
    }

    public static RunService runService(String name, ServiceProperties properties) {
        ControllerProperties<Status> controllerProperties = new ControllerProperties<>(Status.class);
        controllerProperties.actionMetrics((ActionMetrics<Status>) properties.actionMetrics());
        controllerProperties.latencyMetrics((LatencyMetrics<Status>) properties.latencyMetrics());
        controllerProperties.semaphore(properties.semaphore());
        controllerProperties.circuitBreaker(properties.circuitBreaker());

        return new DefaultRunService(new Controller<>(name, controllerProperties));
    }

    public static MultiService defaultService(String name, int poolSize, int concurrencyLevel) {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(concurrencyLevel);
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, properties.concurrencyLevel());
        return new DefaultService(name, executor, properties);
    }

    public static MultiService defaultService(String name, int poolSize, ServiceProperties properties) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, properties.concurrencyLevel());
        return new DefaultService(name, executor, properties);
    }
}
