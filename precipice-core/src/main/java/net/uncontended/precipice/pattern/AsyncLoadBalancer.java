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
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.ThreadPoolService;
import net.uncontended.precipice.utils.MetricCallback;

import java.util.Map;

public class AsyncLoadBalancer<C> implements Controllable<Status> {

    private final ThreadPoolService[] services;
    private final C[] contexts;
    private final LoadBalancerStrategy strategy;
    private final PrecipiceFunction<Status, PerformingContext> metricCallback;
    private final Controller<Status> controller;


    @SuppressWarnings("unchecked")
    public AsyncLoadBalancer(Map<? extends ThreadPoolService, C> executorToContext, LoadBalancerStrategy strategy,
                             Controller<Status> controller) {
        this.controller = controller;
        if (executorToContext.isEmpty()) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        this.strategy = strategy;
        services = new ThreadPoolService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<? extends ThreadPoolService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }
        metricCallback = new MetricCallback(controller.getActionMetrics(), controller.getLatencyMetrics());
    }

    public <T> PrecipiceFuture<Status, T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        long startTime = System.nanoTime();
        Eventual<Status, T> eventual = new Eventual<>(startTime);
        internalComplete(action, eventual, millisTimeout);
        return eventual;
    }

    public <T> void complete(ResilientPatternAction<T, C> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long startTime = System.nanoTime();
        Eventual<Status, T> internalEventual = new Eventual<>(startTime, promise);
        internalComplete(action, internalEventual, millisTimeout);
    }

    public void shutdown() {
        for (ThreadPoolService e : services) {
            e.shutdown();
        }
    }

    private <T> void internalComplete(ResilientPatternAction<T, C> action, Eventual<Status, T> eventual, long millisTimeout) {
        int firstServiceToTry = strategy.nextExecutorIndex();
        final ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        eventual.internalOnComplete(metricCallback);

        int j = 0;
        int serviceCount = services.length;
        while (true) {
            try {
                int serviceIndex = (firstServiceToTry + j) % serviceCount;
                actionWithContext.context = contexts[serviceIndex];
                ThreadPoolService service = services[serviceIndex];
                service.complete(actionWithContext, eventual, millisTimeout);
                break;
            } catch (RejectedException e) {
                ++j;
                if (j == serviceCount) {
                    Rejected reason = Rejected.ALL_SERVICES_REJECTED;
                    controller.getActionMetrics().incrementRejectionCount(reason);
                    throw new RejectedException(reason);
                }
            }
        }
    }

    @Override
    public Controller<Status> controller() {
        return controller;
    }
}
