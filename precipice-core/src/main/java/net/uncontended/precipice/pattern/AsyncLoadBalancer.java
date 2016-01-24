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
import net.uncontended.precipice.concurrent.NewEventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.utils.MetricCallback;

import java.util.Map;

public class AsyncLoadBalancer<C> implements AsyncPattern<C> {

    private final AsyncService[] services;
    private final C[] contexts;
    private final LoadBalancerStrategy strategy;
    private final PrecipiceFunction<Status, PerformingContext> metricCallback;
    private final PatternController<Status> controller;


    @SuppressWarnings("unchecked")
    public AsyncLoadBalancer(Map<? extends AsyncService, C> executorToContext, LoadBalancerStrategy strategy,
                             PatternController<Status> controller) {
        this.controller = controller;
        if (executorToContext.isEmpty()) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        this.strategy = strategy;
        services = new MultiService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<? extends AsyncService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }
        metricCallback = new MetricCallback(controller.getActionMetrics(), controller.getLatencyMetrics());
    }

    public AsyncLoadBalancer(AsyncService[] services, C[] contexts, LoadBalancerStrategy strategy,
                             PatternController<Status> controller) {
        this.strategy = strategy;
        this.services = services;
        this.contexts = contexts;
        this.controller = controller;
        metricCallback = new MetricCallback(controller.getActionMetrics(), controller.getLatencyMetrics());
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        long startTime = System.nanoTime();
        NewEventual<Status, T> eventual = new NewEventual<>(startTime);
        internalComplete(action, eventual, millisTimeout);
        return eventual;
    }

    @Override
    public <T> void complete(ResilientPatternAction<T, C> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long startTime = System.nanoTime();
        NewEventual<Status, T> internalEventual = new NewEventual<>(startTime, promise);
        internalComplete(action, internalEventual, millisTimeout);
    }

    @Override
    public ActionMetrics<Status> getActionMetrics() {
        return controller.getActionMetrics();
    }

    @Override
    public void shutdown() {
        for (AsyncService e : services) {
            e.shutdown();
        }
    }

    private <T> void internalComplete(ResilientPatternAction<T, C> action, NewEventual<Status, T> eventual, long millisTimeout) {
        int firstServiceToTry = strategy.nextExecutorIndex();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        eventual.internalOnComplete(metricCallback);

        int j = 0;
        int serviceCount = services.length;
        while (true) {
            try {
                int serviceIndex = (firstServiceToTry + j) % serviceCount;
                actionWithContext.context = contexts[serviceIndex];
                AsyncService service = services[serviceIndex];
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

}
