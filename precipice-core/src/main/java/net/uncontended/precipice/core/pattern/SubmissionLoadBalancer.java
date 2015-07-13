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

package net.uncontended.precipice.core.pattern;

import net.uncontended.precipice.core.MultiService;
import net.uncontended.precipice.core.RejectedActionException;
import net.uncontended.precipice.core.RejectionReason;
import net.uncontended.precipice.core.SubmissionService;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.Promise;
import net.uncontended.precipice.core.metrics.ActionMetrics;
import net.uncontended.precipice.core.metrics.DefaultActionMetrics;
import net.uncontended.precipice.core.metrics.Metric;

import java.util.Map;

public class SubmissionLoadBalancer<C> extends AbstractPattern<C> implements SubmissionPattern<C> {

    private final SubmissionService[] services;
    private final C[] contexts;
    private final LoadBalancerStrategy strategy;

    public SubmissionLoadBalancer(Map<? extends SubmissionService, C> executorToContext, LoadBalancerStrategy strategy) {
        this(executorToContext, strategy, new DefaultActionMetrics());
    }

    @SuppressWarnings("unchecked")
    public SubmissionLoadBalancer(Map<? extends SubmissionService, C> executorToContext, LoadBalancerStrategy strategy,
                                  ActionMetrics metrics) {
        super(metrics);
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        this.strategy = strategy;
        services = new MultiService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<? extends SubmissionService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }
    }

    public SubmissionLoadBalancer(SubmissionService[] services, C[] contexts, LoadBalancerStrategy strategy,
                                  ActionMetrics metrics) {
        super(metrics);
        this.strategy = strategy;
        this.services = services;
        this.contexts = contexts;
    }

    @Override
    public <T> PrecipiceFuture<T> complete(ResilientPatternAction<T, C> action, long millisTimeout) {
        int firstServiceToTry = strategy.nextExecutorIndex();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        int j = 0;
        int serviceCount = services.length;
        while (true) {
            try {
                int serviceIndex = (firstServiceToTry + j) % serviceCount;
                actionWithContext.context = contexts[serviceIndex];
                return services[serviceIndex].submit(actionWithContext, millisTimeout);
            } catch (RejectedActionException e) {
                ++j;
                if (j == serviceCount) {
                    metrics.incrementMetricCount(Metric.ALL_SERVICES_REJECTED);
                    throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
                }
            }
        }
    }

    @Override
    public <T> void complete(ResilientPatternAction<T, C> action, Promise<T> promise, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        for (SubmissionService e : services) {
            e.shutdown();
        }
    }

}
