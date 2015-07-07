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

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Servers {

    private List<Undertow> servers = new ArrayList<>();


    public Servers() {
        servers.add(create(6001, new Handler1()));
        servers.add(create(7001, new Handler2()));
    }

    public void start() {
        for (Undertow server : servers) {
            server.start();
        }
    }

    public void stop() {
        for (Undertow server : servers) {
            server.stop();
        }
    }

    private Undertow create(int port, HttpHandler handler) {
        return Undertow.builder().addHttpListener(port, "127.0.0.1").setHandler(handler).build();
    }

    private static class Handler1 implements HttpHandler {

        AtomicLong lastRequestTime = new AtomicLong(0);
        AtomicInteger count = new AtomicInteger(0);

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            long currentTime = System.currentTimeMillis();
            long lastRequestTime = this.lastRequestTime.getAndSet(currentTime);
            if (currentTime - lastRequestTime < 50) {
                exchange.setResponseCode(500);
            } else {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                int i = count.incrementAndGet();
                if (i > 20 && i < 30) {
                    Thread.sleep(70);
                }
                exchange.getResponseSender().send("Server1 Response: " + i);
            }
        }
    }

    private static class Handler2 implements HttpHandler {

        AtomicLong lastRequestTime = new AtomicLong(0);
        AtomicInteger count = new AtomicInteger(0);

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            long currentTime = System.currentTimeMillis();
            long lastRequestTime = this.lastRequestTime.getAndSet(currentTime);
            if (currentTime - lastRequestTime < 50) {
                exchange.setResponseCode(500);
            } else {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                int i = count.incrementAndGet();
                if (i > 30 && i < 40) {
                    Thread.sleep(70);
                }
                exchange.getResponseSender().send("Server2 Response: " + i);
            }

        }
    }
}
