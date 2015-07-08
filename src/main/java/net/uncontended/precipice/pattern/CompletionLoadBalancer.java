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
import net.uncontended.precipice.concurrent.ResilientPromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.Metric;

import java.util.Map;

public class CompletionLoadBalancer<C> extends AbstractPattern<C> implements CompletionPattern<C> {

    private final CompletionService[] services;
    private final C[] contexts;
    private final LoadBalancerStrategy strategy;

    public CompletionLoadBalancer(Map<? extends CompletionService, C> executorToContext, LoadBalancerStrategy strategy) {
        this(executorToContext, strategy, new DefaultActionMetrics());
    }

    @SuppressWarnings("unchecked")
    public CompletionLoadBalancer(Map<? extends CompletionService, C> executorToContext, LoadBalancerStrategy strategy,
                                  ActionMetrics metrics) {
        super(metrics);
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        this.strategy = strategy;
        services = new MultiService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<? extends CompletionService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }
    }

    public CompletionLoadBalancer(ActionMetrics metrics, CompletionService[] services, C[] contexts,
                                  LoadBalancerStrategy strategy) {
        super(metrics);
        this.strategy = strategy;
        this.services = services;
        this.contexts = contexts;
    }

    @Override
    public <T> void submitAndComplete(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      long millisTimeout) {
        submitAndComplete(action, promise, null, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(final ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      ResilientCallback<T> callback, long millisTimeout) {
        final int firstServiceToTry = strategy.nextExecutorIndex();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        int j = 0;
        int serviceCount = services.length;
        while (true) {
            try {
                int serviceIndex = (firstServiceToTry + j) % serviceCount;
                actionWithContext.context = contexts[serviceIndex];
                MetricsCallback<T> metricsCallback = new MetricsCallback<>(metrics, callback);
                services[serviceIndex].submitAndComplete(actionWithContext, promise, metricsCallback, millisTimeout);
                break;
            } catch (RejectedActionException e) {
                ++j;
                if (j == serviceCount) {
                    throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
                }
            }
        }

    }

    @Override
    public void shutdown() {
        for (CompletionService e : services) {
            e.shutdown();
        }
    }

    private static class MetricsCallback<T> implements ResilientCallback<T> {

        private final ActionMetrics metrics;
        private final ResilientCallback<T> callback;

        public MetricsCallback(ActionMetrics metrics, ResilientCallback<T> callback) {
            this.metrics = metrics;
            this.callback = callback;
        }

        @Override
        public void run(ResilientPromise<T> resultPromise) {
            metrics.incrementMetricCount(Metric.statusToMetric(resultPromise.getStatus()));
            if (callback != null) {
                callback.run(resultPromise);
            }
        }
    }
}
