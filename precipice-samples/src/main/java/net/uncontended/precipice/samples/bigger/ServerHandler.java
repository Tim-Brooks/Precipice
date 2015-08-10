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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServerHandler implements HttpHandler {

    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    private final AtomicInteger millisBetweenSuccess = new AtomicInteger(1);
    private final AtomicLong errorUntilTime = new AtomicLong(0L);
    private final AtomicInteger successesPerTimeout = new AtomicInteger(100);

    public ServerHandler(String name) {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new ServerConfigs() {
                @Override
                public int getMillisBetweenSuccess() {
                    return millisBetweenSuccess.get();
                }

                @Override
                public void setMillisBetweenSuccess(int milliseconds) {
                    millisBetweenSuccess.set(milliseconds);
                }


                @Override
                public void setMillisSecondsToError(int milliseconds) {
                    errorUntilTime.set(System.currentTimeMillis() + milliseconds);
                }

                @Override
                public int getSuccessesPerTimeout() {
                    return successesPerTimeout.get();
                }

                @Override
                public void setSuccessesPerTimeout(int successes) {
                    successesPerTimeout.set(successes);
                }
            }, new ObjectName(String.format("net.uncontended.precipice:type=ServerHandler,name=%s", name)));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException |
                MalformedObjectNameException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        long currentTime = System.currentTimeMillis();
        long lastRequestTime = this.lastRequestTime.getAndSet(currentTime);
        if (currentTime - lastRequestTime < millisBetweenSuccess.get() || currentTime < errorUntilTime.get()) {
            exchange.setResponseCode(500);
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            int i = count.incrementAndGet();
            if (i % successesPerTimeout.get() == 0) {
                Thread.sleep(30);
            }
            exchange.getResponseSender().send("Server1 Response: " + i);
        }
    }

    @MXBean
    public interface ServerConfigs {
        int getMillisBetweenSuccess();

        void setMillisBetweenSuccess(int milliseconds);

        void setMillisSecondsToError(int milliseconds);

        int getSuccessesPerTimeout();

        void setSuccessesPerTimeout(int successesPerTimeout);
    }


}
