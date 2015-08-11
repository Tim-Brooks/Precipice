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

package net.uncontended.precipice.samples;

import net.uncontended.precipice.MultiService;
import net.uncontended.precipice.ServiceProperties;
import net.uncontended.precipice.Services;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.pattern.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class LoadBalancerExample {

    private final String serviceName1;
    private final String serviceName2;
    private final int poolSize;
    private final int concurrencyLevel;
    private final Map<MultiService, Map<String, String>> serviceToContext;

    public LoadBalancerExample() {
        serviceName1 = "Identity Service1";
        serviceName2 = "Identity Service2";
        poolSize = 5;
        concurrencyLevel = 100;
        MultiService service1 = Services.defaultService(serviceName1, poolSize, concurrencyLevel);
        Map<String, String> context1 = new HashMap<>();
        context1.put("address", "127.0.0.1");
        context1.put("port", "6001");
        MultiService service2 = Services.defaultService(serviceName2, poolSize, concurrencyLevel);
        Map<String, String> context2 = new HashMap<>();
        context2.put("address", "127.0.0.1");
        context2.put("port", "6002");

        serviceToContext = new HashMap<>();
        serviceToContext.put(service1, context1);
        serviceToContext.put(service2, context2);

    }

    public void balancerExample() throws InterruptedException {
        MultiPattern<Map<String, String>> balancer = LoadBalancers.multiRoundRobin(serviceToContext);

        // Will complete the action to one of the services. If all of the services reject the action,
        // this will throw a RejectedActionException with RejectionReason ALL_SERVICES_REJECTED.
        PrecipiceFuture<String> f = balancer.submit(new ResilientPatternAction<String, Map<String, String>>() {
            @Override
            public String run(Map<String, String> context) throws Exception {
                return context.get("port");
            }
        }, 100L);

        try {
            // Should return the port (6001 or 6002) of the service to which the action was sent
            f.get();
        } catch (ExecutionException e) {
            e.getCause().printStackTrace();
        }
    }

    public void specializedExample() {
        AsyncPattern<Map<String, String>> submission = LoadBalancers.asyncRoundRobin(serviceToContext);
        PrecipiceFuture<String> f = submission.submit(new ImplementedPatternAction(), 100L);

        RunPattern<Map<String, String>> run = LoadBalancers.runRoundRobin(serviceToContext);
        try {
            run.run(new ImplementedPatternAction());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void userStrategyExample() {
        LoadBalancerStrategy strategy = new LoadBalancerStrategy() {
            @Override
            public int nextExecutorIndex() {
                // Implement your strategy
                return 0;
            }
        };

        MultiPattern<Map<String, String>> balancerWithUserStrategy = new MultiLoadBalancer<>(serviceToContext, strategy);
    }

    public void sharedThreadpool() {
        List<Map<String, String>> contexts = new ArrayList<>();
        contexts.addAll(serviceToContext.values());
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(concurrencyLevel);
        MultiPattern<Map<String, String>> balancer = LoadBalancers.multiRoundRobinWithSharedPool(contexts,
                "Identity Service", poolSize, properties);
    }

    private static class ImplementedPatternAction implements ResilientPatternAction<String, Map<String, String>> {

        @Override
        public String run(Map<String, String> context) throws Exception {
            return "Done";
        }
    }
}
