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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.utils.MetricCallback;

import java.util.Map;

public class AsyncLoadBalancer<C> extends AbstractPattern<C> implements AsyncPattern<C> {

    private final AsyncService[] services;
    private final C[] contexts;
    private final LoadBalancerStrategy strategy;
    private final PrecipiceFunction<SuperImpl, Void> metricCallback = new MetricCallback(metrics);


    public AsyncLoadBalancer(Map<? extends AsyncService, C> executorToContext, LoadBalancerStrategy strategy) {
        this(executorToContext, strategy, new DefaultActionMetrics<>(SuperImpl.class));
    }

    @SuppressWarnings("unchecked")
    public AsyncLoadBalancer(Map<? extends AsyncService, C> executorToContext, LoadBalancerStrategy strategy,
                             ActionMetrics<SuperImpl> metrics) {
        super(metrics);
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
    }

    public AsyncLoadBalancer(AsyncService[] services, C[] contexts, LoadBalancerStrategy strategy,
                             ActionMetrics<SuperImpl> metrics) {
        super(metrics);
        this.strategy = strategy;
        this.services = services;
        this.contexts = contexts;
    }

    @Override
    public <T> PrecipiceFuture<SuperImpl, T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        Eventual<SuperImpl, T> eventual = new Eventual<>();
        internalComplete(action, eventual, millisTimeout);
        return eventual;
    }

    @Override
    public <T> void complete(ResilientPatternAction<T, C> action, PrecipicePromise<SuperImpl, T> promise, long millisTimeout) {
        Eventual<SuperImpl, T> internalEventual = new Eventual<>(promise);
        internalComplete(action, internalEventual, millisTimeout);
    }

    @Override
    public void shutdown() {
        for (AsyncService e : services) {
            e.shutdown();
        }
    }

    private <T> void internalComplete(ResilientPatternAction<T, C> action, Eventual<SuperImpl, T> eventual, long millisTimeout) {
        int firstServiceToTry = strategy.nextExecutorIndex();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        eventual.internalOnSuccess(metricCallback);
        eventual.internalOnError(metricCallback);

        int j = 0;
        int serviceCount = services.length;
        while (true) {
            try {
                int serviceIndex = (firstServiceToTry + j) % serviceCount;
                actionWithContext.context = contexts[serviceIndex];
                services[serviceIndex].complete(actionWithContext, eventual, millisTimeout);
                break;
            } catch (RejectedActionException e) {
                ++j;
                if (j == serviceCount) {
                    metrics.incrementMetricCount(SuperImpl.ALL_SERVICES_REJECTED);
                    throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
                }
            }
        }
    }

}
