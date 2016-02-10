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

package net.uncontended.precipice.samples.bigger;

import com.squareup.okhttp.*;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.ControllerProperties;
import net.uncontended.precipice.RejectedException;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.metrics.DefaultCountMetrics;
import net.uncontended.precipice.pattern.PatternAction;
import net.uncontended.precipice.pattern.PatternStrategy;
import net.uncontended.precipice.pattern.RoundRobinLoadBalancer;
import net.uncontended.precipice.threadpool.ThreadPoolPattern;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Client {
//
//    private final ThreadPoolPattern<Map<String, Object>> loadBalancer;
//    private final OkHttpClient client = new OkHttpClient();
//    private final List<ClientMBeans> clientMBeans = new ArrayList<>();
//
//    public Client() {
//        Map<ThreadPoolService, Map<String, Object>> services = new HashMap<>();
//        addServiceToMap(services, "Weather-1", 6001);
//        addServiceToMap(services, "Weather-2", 7001);
//
//        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
//        properties.actionMetrics(new DefaultCountMetrics<>(Status.class, 20, 500, TimeUnit.MILLISECONDS));
//        PatternStrategy strategy = new RoundRobinLoadBalancer(services.size());
//        loadBalancer = null;
////        loadBalancer = new ThreadPoolLoadBalancer<>(services, null);
//
//        clientMBeans.add(new ClientMBeans("LoadBalancer", loadBalancer.guardRail().getResultMetrics()));
//    }
//
//    public void run() throws InterruptedException {
//        for (int i = 0; i < Integer.MAX_VALUE; ++i) {
//            Thread.sleep(20);
//
//            try {
//                PrecipiceFuture<Status, String> f = loadBalancer.submit(new Action(), 20L);
//                String result = f.get();
//                Status status = f.getStatus();
//
//
//                System.out.println(result);
//                System.out.println(status);
//
//            } catch (RejectedException e) {
//                System.out.println(e.reason);
//            } catch (ExecutionException e) {
//                System.out.println("ERROR");
//                System.out.println(e.getCause().getMessage());
//            }
//        }
//
//        loadBalancer.shutdown();
//    }
//
//
//    private void addServiceToMap(Map<ThreadPoolService, Map<String, Object>> services, String name, int port) {
//        BreakerConfigBuilder builder = new BreakerConfigBuilder()
//                .backOffTimeMillis(2000)
//                .trailingPeriodMillis(3000);
//        DefaultCountMetrics<Status> actionMetrics = new DefaultCountMetrics<>(Status.class, 20, 500, TimeUnit.MILLISECONDS);
//        DefaultCircuitBreaker breaker = new DefaultCircuitBreaker(builder.build());
//        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
//        properties.actionMetrics(actionMetrics);
//        properties.circuitBreaker(breaker);
//        Controller<Status> controller = new Controller<>(name, properties);
//        final ThreadPoolService service = new ThreadPoolService(5, controller);
//        Map<String, Object> context = new HashMap<>();
//        context.put("host", "127.0.0.1");
//        context.put("port", port);
//        services.put(service, context);
//
//        clientMBeans.add(new ClientMBeans(name, actionMetrics, breaker));
//    }
//
//    private class Action implements PatternAction<String, Map<String, Object>> {
//        @Override
//        public String call(Map<String, Object> context) throws Exception {
//            HttpUrl.Builder builder = new HttpUrl.Builder().scheme("http");
//            HttpUrl url = builder.port((int) context.get("port")).host((String) context.get("host")).build();
//            Request request = new Request.Builder().url(url).build();
//            Response response = client.newCall(request).execute();
//            if (response.code() != 500) {
//                ResponseBody body = response.body();
//                return body.string();
//            } else {
//                throw new RuntimeException("Server Error");
//            }
//
//        }
//    }
}
