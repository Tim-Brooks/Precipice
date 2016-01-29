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
package net.uncontended.precipice.pattern;

import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class Shotgun<C> {

    private final ShotgunStrategy strategy;
    private final Controller<Status> controller;
    private final NewShotgun<Status, ThreadPoolService> newShotgun;
    private final Map<ThreadPoolService, C> serviceToContext;

    public Shotgun(Map<ThreadPoolService, C> serviceToContext, int submissionCount, Controller<Status> controller) {
        this(serviceToContext, submissionCount, controller, new ShotgunStrategy(serviceToContext.size(), submissionCount));
    }

    @SuppressWarnings("unchecked")
    public Shotgun(Map<ThreadPoolService, C> serviceToContext, int submissionCount, Controller<Status> controller,
                   ShotgunStrategy strategy) {
        if (serviceToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > serviceToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be greater than the number of services " +
                    "provided.");
        }

        this.serviceToContext = serviceToContext;
        this.controller = controller;
        List<ThreadPoolService> services = new ArrayList<>(serviceToContext.size());
        for (Map.Entry<ThreadPoolService, C> entry : serviceToContext.entrySet()) {
            services.add(entry.getKey());
        }

        this.newShotgun = new NewShotgun<>(controller, services, strategy);
        this.strategy = strategy;
    }

    public <T> PrecipiceFuture<Status, T> submit(final PatternAction<T, C> action, long millisTimeout) {
        PatternEntry<ThreadPoolService, PrecipicePromise<Status, T>>[] patternEntries = newShotgun.promisePair();

        for (PatternEntry<ThreadPoolService, PrecipicePromise<Status, T>> entry : patternEntries) {
            if (entry != null) {
                final C context = serviceToContext.get(entry.controllable);
                Callable<T> callable = new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        return action.run(context);
                    }
                };
                // TODO: Add executor service getter
                entry.controllable.bypassBackPressureAndComplete(callable, entry.completable, millisTimeout);
            }
        }

        return patternEntries[0].completable.future();
    }

    public void shutdown() {
        for (ThreadPoolService service : serviceToContext.keySet()) {
            service.shutdown();
        }

    }
}
