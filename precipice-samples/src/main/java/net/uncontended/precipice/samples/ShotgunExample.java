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

import net.uncontended.precipice.Controller;
import net.uncontended.precipice.ControllerProperties;
import net.uncontended.precipice.Services;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.pattern.PatternAction;
import net.uncontended.precipice.pattern.Shotgun;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ShotgunExample {

    private final Map<ThreadPoolService, Map<String, String>> serviceToContext;

    public ShotgunExample() {
        serviceToContext = new HashMap<>();

        int concurrencyLevel = 100;

        ThreadPoolService service1 = Services.submissionService("Service1", 10, concurrencyLevel);
        Map<String, String> context1 = new HashMap<>();
        context1.put("port", "6001");
//        serviceToContext.put(service1, context1);

        ThreadPoolService service2 = Services.submissionService("Service2", 10, concurrencyLevel);
        Map<String, String> context2 = new HashMap<>();
        context2.put("port", "6002");
//        serviceToContext.put(service2, context2);

        ThreadPoolService service3 = Services.submissionService("Service1", 10, concurrencyLevel);
        Map<String, String> context3 = new HashMap<>();
        context3.put("port", "6003");
//        serviceToContext.put(service3, context3);

    }

    public void shotgunExample() throws InterruptedException {
        Controller<Status> controller = new Controller<>("shotgun", new ControllerProperties<>(Status.class));
        Shotgun<Map<String, String>> shotgun = new Shotgun<>(serviceToContext, 2, controller);

        // Will complete the action to two of the services. If all of the services reject the action,
        // this will throw a RejectedException with Rejected ALL_SERVICES_REJECTED.
        PrecipiceFuture<Status, String> f = shotgun.submit(new PatternAction<String, Map<String, String>>() {
            @Override
            public String run(Map<String, String> context) throws Exception {
                return context.get("port");
            }
        }, 100L);

        try {
            // Should return the port number of the first service to complete the action.
            f.get();
        } catch (ExecutionException e) {
            e.getCause().printStackTrace();
        }

    }
}
