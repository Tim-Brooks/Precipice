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

package net.uncontended.precipice.core.samples.bigger;

import com.squareup.okhttp.*;
import net.uncontended.precipice.core.*;
import net.uncontended.precipice.core.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.core.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.metrics.DefaultActionMetrics;
import net.uncontended.precipice.core.pattern.LoadBalancers;
import net.uncontended.precipice.core.pattern.ResilientPatternAction;
import net.uncontended.precipice.core.pattern.SubmissionPattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Client {

    private final SubmissionPattern<Map<String, Object>> loadBalancer;
    private final OkHttpClient client = new OkHttpClient();
    private final List<ClientMBeans> clientMBeans = new ArrayList<>();

    public Client() {
        Map<SubmissionService, Map<String, Object>> services = new HashMap<>();
        addServiceToMap(services, "Weather-1", 6001);
        addServiceToMap(services, "Weather-2", 7001);

        loadBalancer = LoadBalancers.submittingRoundRobin(services, new DefaultActionMetrics(20, 500, TimeUnit.MILLISECONDS));

        clientMBeans.add(new ClientMBeans("LoadBalancer", loadBalancer.getActionMetrics()));
    }

    public void run() throws InterruptedException {
        for (int i = 0; i < Integer.MAX_VALUE; ++i) {
            Thread.sleep(20);

            try {
                PrecipiceFuture<String> f = loadBalancer.complete(new Action(), 20L);
                String result = f.get();
                Status status = f.getStatus();


                System.out.println(result);
                System.out.println(status);

            } catch (RejectedActionException e) {
                System.out.println(e.reason);
            } catch (ExecutionException e) {
                System.out.println("ERROR");
                System.out.println(e.getCause().getMessage());
            }
        }

        loadBalancer.shutdown();
    }


    private void addServiceToMap(Map<SubmissionService, Map<String, Object>> services, String name, int port) {
        BreakerConfigBuilder builder = new BreakerConfigBuilder()
                .backOffTimeMillis(2000)
                .trailingPeriodMillis(3000);
        DefaultActionMetrics actionMetrics = new DefaultActionMetrics(20, 500, TimeUnit.MILLISECONDS);
        DefaultCircuitBreaker breaker = new DefaultCircuitBreaker(builder.build());
        ServiceProperties properties = new ServiceProperties();
        properties.actionMetrics(actionMetrics);
        properties.circuitBreaker(breaker);
        properties.concurrencyLevel(20);
        final SubmissionService service = Services.defaultService(name, 5, properties);
        Map<String, Object> context = new HashMap<>();
        context.put("host", "127.0.0.1");
        context.put("port", port);
        services.put(service, context);

        clientMBeans.add(new ClientMBeans(name, actionMetrics, breaker));
    }

    private class Action implements ResilientPatternAction<String, Map<String, Object>> {
        @Override
        public String run(Map<String, Object> context) throws Exception {
            HttpUrl.Builder builder = new HttpUrl.Builder().scheme("http");
            HttpUrl url = builder.port((int) context.get("port")).host((String) context.get("host")).build();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (response.code() != 500) {
                ResponseBody body = response.body();
                return body.string();
            } else {
                throw new RuntimeException("Server Error");
            }

        }
    }
}
