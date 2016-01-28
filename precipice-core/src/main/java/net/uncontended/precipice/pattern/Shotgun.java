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
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.RejectedException;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.Map;

public class Shotgun<C> {

    private final ThreadPoolService[] services;
    private final ShotgunStrategy strategy;
    private final C[] contexts;
    private final Controller<Status> controller;

    public Shotgun(Map<ThreadPoolService, C> executorToContext, int submissionCount, Controller<Status> controller) {
        this(executorToContext, submissionCount, controller, new ShotgunStrategy(executorToContext.size(), submissionCount));
    }

    @SuppressWarnings("unchecked")
    public Shotgun(Map<ThreadPoolService, C> executorToContext, int submissionCount, Controller<Status> controller,
                   ShotgunStrategy strategy) {
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > executorToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be greater than the number of services " +
                    "provided.");
        }

        this.controller = controller;
        services = new ThreadPoolService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<ThreadPoolService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }

        this.strategy = strategy;
    }

    public <T> PrecipiceFuture<Status, T> submit(PatternAction<T, C> action, long millisTimeout) {
        int[] servicesToTry = strategy.executorIndices();
        PrecipicePromise<Status, T> promise = controller.acquirePermitAndGetPromise();

        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            try {
                ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);
                actionWithContext.context = contexts[serviceIndex];
                ThreadPoolService service = services[serviceIndex];

                PrecipicePromise<Status, T> servicePromise = service.controller().acquirePermitAndGetPromise(promise);
                service.bypassBackPressureAndComplete(actionWithContext, servicePromise, millisTimeout);
                ++submittedCount;
            } catch (RejectedException e) {
            }
            if (submittedCount == strategy.getSubmissionCount()) {
                break;
            }
        }
        if (submittedCount == 0) {
            controller.getSemaphore().releasePermit(1);
            controller.getActionMetrics().incrementRejectionCount(Rejected.ALL_SERVICES_REJECTED);
            throw new RejectedException(Rejected.ALL_SERVICES_REJECTED);
        }

        return promise.future();
    }

    public void shutdown() {
        for (ThreadPoolService service : services) {
            service.shutdown();
        }

    }
}
