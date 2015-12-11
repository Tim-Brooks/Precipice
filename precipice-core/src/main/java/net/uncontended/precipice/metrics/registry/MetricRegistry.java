/*
 * Copyright 2015 Timothy Brooks
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
 */

package net.uncontended.precipice.metrics.registry;

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MetricRegistry {

    private final long period;
    private final TimeUnit unit;
    private final Map<String, Summary> services = new ConcurrentHashMap<>();
    private volatile PrecipiceFunction<Map<String, Summary>> callback;
    private final ScheduledExecutorService executorService;

    public MetricRegistry(long period, TimeUnit unit) {
        this.unit = unit;
        this.period = period;

        executorService = Executors.newSingleThreadScheduledExecutor(new RegistryThreadFactory());
        executorService.scheduleAtFixedRate(new Task(), 0, period, unit);
    }

    public void register(Service service) {
        services.put(service.getName(), new Summary(period, unit, service));
    }

    public boolean deregister(String name) {
        return null == services.remove(name);
    }

    public void setUpdateCallback(PrecipiceFunction<Map<String, Summary>> callback) {
        this.callback = callback;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private class Task implements Runnable {

        @Override
        public void run() {
            for (Summary summary : services.values()) {
                summary.refresh();
            }

            if (callback != null) {
                callback.apply(services);
            }
        }
    }

    private static class RegistryThreadFactory implements ThreadFactory {
        private static final AtomicLong counter = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("metric-registry-thread-" + counter.incrementAndGet());
            return thread;
        }
    }
}
