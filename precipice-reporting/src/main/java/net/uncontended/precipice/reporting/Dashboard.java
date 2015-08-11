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

package net.uncontended.precipice.reporting;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.OutputStream;

public class Dashboard implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        } else {
            exchange.startBlocking();
        }
        OutputStream outputStream = exchange.getOutputStream();
        outputStream.write("[".getBytes());
        for (int i = 0; i < 5; ++i) {
            if (i != 4) {
                outputStream.write(String.format("{\"time\": %s, \"y\": %s},", System.currentTimeMillis(), i)
                        .getBytes());
            } else {
                outputStream.write(String.format("{\"time\": %s, \"y\": %s}", System.currentTimeMillis(), i)
                        .getBytes());

            }
            Thread.sleep(500);
        }
        outputStream.write("]".getBytes());
        outputStream.close();
    }
}
