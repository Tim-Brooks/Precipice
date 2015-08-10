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

import net.uncontended.precipice.core.*;
import net.uncontended.precipice.core.metrics.ActionMetrics;
import net.uncontended.precipice.core.utils.PrecipiceExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class LoadBalancers {

    public static <C> MultiPattern<C> multiRoundRobin(Map<MultiService, C> serviceToContext) {
        return new MultiLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> MultiPattern<C> multiRoundRobin(Map<MultiService, C> serviceToContext, ActionMetrics metrics) {
        return new MultiLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()), metrics);
    }

    public static <C> SubmissionPattern<C> submittingRoundRobin(Map<? extends AsyncService, C> serviceToContext) {
        return new SubmissionLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> SubmissionPattern<C> submittingRoundRobin(Map<? extends AsyncService, C> serviceToContext,
                                                                ActionMetrics metrics) {
        return new SubmissionLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()), metrics);
    }

    public static <C> RunPattern<C> runRoundRobin(Map<? extends RunService, C> serviceToContext) {
        return new RunLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> RunPattern<C> runRoundRobin(Map<? extends RunService, C> serviceToContext, ActionMetrics metrics) {
        return new RunLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()), metrics);
    }

    public static <C> MultiPattern<C> multiRoundRobinWithSharedPool(List<C> contexts, String name, int poolSize,
                                                                    ServiceProperties properties) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, properties.concurrencyLevel());
        Map<MultiService, C> serviceToContext = new HashMap<>();
        int i = 0;
        for (C context : contexts) {
            ServiceProperties serviceProperties = new ServiceProperties();
            serviceProperties.concurrencyLevel(properties.concurrencyLevel());
            MultiService service = new DefaultService(name + "-" + i, executor, serviceProperties);
            serviceToContext.put(service, context);
            ++i;
        }
        return new MultiLoadBalancer<>(serviceToContext, new RoundRobinStrategy(contexts.size()));
    }
}
