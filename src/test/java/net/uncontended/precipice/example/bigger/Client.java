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

package net.uncontended.precipice.example.bigger;

import com.squareup.okhttp.*;
import net.uncontended.precipice.Services;
import net.uncontended.precipice.SubmissionService;
import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.pattern.LoadBalancers;
import net.uncontended.precipice.pattern.ResilientPatternAction;
import net.uncontended.precipice.pattern.SubmissionPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Client {

    private final SubmissionPattern<Map<String, Object>> loadBalancer;
    private final OkHttpClient client = new OkHttpClient();

    public Client() {
        Map<SubmissionService, Map<String, Object>> services = new HashMap<>();
        addServiceToMap(services, "Weather-1", 6001);
        addServiceToMap(services, "Weather-2", 7001);

        loadBalancer = LoadBalancers.submittingRoundRobin(services);
    }

    public void run() throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            ResilientFuture<String> f = loadBalancer.submit(new ResilientPatternAction<String, Map<String, Object>>() {
                @Override
                public String run(Map<String, Object> context) throws Exception {
                    HttpUrl.Builder builder = new HttpUrl.Builder().scheme("http");
                    HttpUrl url = builder.port((int) context.get("port")).host((String) context.get("host")).build();
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    ResponseBody body = response.body();
                    return body.string();
                }
            }, 100L);

            try {
                System.out.println(f.get());
                System.out.println(f.getStatus());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        loadBalancer.shutdown();
    }


    private void addServiceToMap(Map<SubmissionService, Map<String, Object>> services, String name, int port) {
        SubmissionService service = Services.defaultService(name, 5, 20);
        Map<String, Object> context = new HashMap<>();
        context.put("host", "127.0.0.1");
        context.put("port", port);
        services.put(service, context);
    }
}
