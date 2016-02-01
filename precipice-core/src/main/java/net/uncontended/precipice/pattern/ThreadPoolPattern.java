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

import net.uncontended.precipice.*;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.ThreadPoolService;
import net.uncontended.precipice.threadpool.ThreadPoolTask;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadPoolPattern<C> implements Controllable<Status> {

    private final Controller<Status> controller;
    private final Shotgun<Status, ThreadPoolService> shotgun;
    private final Map<ThreadPoolService, C> serviceToContext;

    public ThreadPoolPattern(Map<ThreadPoolService, C> serviceToContext, int submissionCount, Controller<Status> controller) {
        this(serviceToContext, submissionCount, controller, new ShotgunStrategy(serviceToContext.size(), submissionCount));
    }

    @SuppressWarnings("unchecked")
    public ThreadPoolPattern(Map<ThreadPoolService, C> serviceToContext, int submissionCount, Controller<Status> controller,
                             ShotgunStrategy strategy) {
        if (serviceToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create ThreadPoolPattern with 0 Executors.");
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

        this.shotgun = new Shotgun<>(services, strategy);
    }

    public Controller<Status> controller() {
        return controller;
    }

    public <T> PrecipiceFuture<Status, T> submit(final PatternAction<T, C> action, long millisTimeout) {
        long nanoTime = acquirePermit();

        // TODO: Add a someSubmitted? check
        Iterable<ThreadPoolService> services = shotgun.getControllables(nanoTime);

        PrecipicePromise<Status, T> promise = controller.getPromise(nanoTime);
        long adjustedTimeout = TimeoutService.adjustTimeout(millisTimeout);
        for (ThreadPoolService service : services) {
            PrecipicePromise<Status, T> internal = service.controller().getPromise(nanoTime, promise);

            final C context = serviceToContext.get(service);
            ExecutorService executor = service.getExecutor();
            TimeoutService timeoutService = service.getTimeoutService();

            Callable<T> callable = new CallableWithContext<>(action, context);
            ThreadPoolTask<T> task = new ThreadPoolTask<>(callable, internal, adjustedTimeout, nanoTime);
            executor.execute(task);
            timeoutService.scheduleTimeout(task);
        }

        return promise.future();
    }

    private long acquirePermit() {
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        long nanoTime = System.nanoTime();
        if (rejected != null) {
            controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            throw new RejectedException(rejected);
        }
        return nanoTime;
    }

    public void shutdown() {
        for (ThreadPoolService service : serviceToContext.keySet()) {
            service.shutdown();
        }
    }
}
