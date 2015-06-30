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
import net.uncontended.precipice.circuit.BreakerConfig;
import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class LoadBalancers {

    public static <C> MultiPattern<C> multiRoundRobin(Map<MultiService, C> serviceToContext) {
        return new MultiLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> SubmissionPattern<C> submittingRoundRobin(Map<? extends SubmissionService, C> serviceToContext) {
        return new SubmissionLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> CompletionPattern<C> completingRoundRobin(Map<? extends CompletionService, C> serviceToContext) {
        return new CompletionLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> RunPattern<C> runRoundRobin(Map<? extends RunService, C> serviceToContext) {
        return new RunLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> MultiPattern<C> multiRoundRobinWithSharedPool(List<C> contexts, String name, int poolSize, int
            concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        Map<MultiService, C> serviceToContext = new HashMap<>();
        for (C context : contexts) {
            BreakerConfig configBuilder = new BreakerConfigBuilder().build();
            ActionMetrics metrics = new DefaultActionMetrics();
            MultiService service = Services.defaultService(executor, concurrencyLevel, metrics,
                    new DefaultCircuitBreaker(metrics, configBuilder));
            serviceToContext.put(service, context);
        }
        return new MultiLoadBalancer<>(serviceToContext, new RoundRobinStrategy(contexts.size()));
    }
}
