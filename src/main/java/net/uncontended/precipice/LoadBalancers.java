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
package net.uncontended.precipice;

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

    public static <C> SubmitPattern<C> newRoundRobin(Map<DefaultService, C> serviceToContext) {
        return new LoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()));
    }

    public static <C> SubmitPattern<C> newRoundRobinWithSharedPool(List<C> contexts, String name, int poolSize, int
            concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        Map<DefaultService, C> serviceToContext = new HashMap<>();
        for (C context : contexts) {
            BreakerConfig configBuilder = new BreakerConfigBuilder().build();
            ActionMetrics metrics = new DefaultActionMetrics();
            DefaultService service = (DefaultService) Services.defaultService(executor, concurrencyLevel, metrics,
                    new DefaultCircuitBreaker(metrics, configBuilder));
            serviceToContext.put(service, context);
        }
        return new LoadBalancer<>(serviceToContext, new RoundRobinStrategy(contexts.size()));
    }
}
