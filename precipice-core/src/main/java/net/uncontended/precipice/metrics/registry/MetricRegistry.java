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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricRegistry {

    private Map<String, Summary> services = new HashMap<>();
    private volatile PrecipiceFunction<Map<String, Summary>> callback;
    // TODO: Name thread
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MetricRegistry() {
        executorService.scheduleAtFixedRate(new Task(), 0, 10, TimeUnit.SECONDS);
    }

    public void register(Service service) {
        services.put(service.getName(), new Summary(service));
    }

    public Summary getSummary(String name) {
        Summary summary = services.get(name);
        if (summary == null) {
            throw new IllegalArgumentException("Service: " + name + " not registered.");
        }
        return summary;
    }

    public void setUpdateCallback(PrecipiceFunction<Map<String, Summary>> callback) {
        this.callback = callback;
    }

    public void shutdown() {
        executorService.shutdown();
    }


    private static class Summary {
        private final Service service;

        private Summary(Service service) {
            this.service = service;
        }

        private void refresh() {

        }
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
}
