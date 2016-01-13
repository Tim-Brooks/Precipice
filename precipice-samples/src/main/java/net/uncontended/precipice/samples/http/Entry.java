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

package net.uncontended.precipice.samples.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import net.uncontended.precipice.ServiceProperties;
import net.uncontended.precipice.SuperImpl;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.metrics.ActionMetrics;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Entry {
    public static void main(String[] args) {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        HttpAsyncService service = new HttpAsyncService("Hello", new ServiceProperties(), asyncHttpClient);
        Request request = new RequestBuilder().setUrl("http://www.google.com").setRequestTimeout(100).build();

        PrecipiceFuture<SuperImpl, Object> f = service.submit(new ServiceRequest<Object>(request) {
            @Override
            public Object run() throws Exception {
                return response;
            }
        }, 100L);

        try {
            System.out.println(f.get());
            System.out.println(f.getStatus());
            ActionMetrics<SuperImpl> actionMetrics = (ActionMetrics<SuperImpl>) service.getActionMetrics();
            System.out.println(actionMetrics.getMetricCountForTimePeriod(SuperImpl.SUCCESS, 100, TimeUnit
                    .SECONDS));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        service.shutdown();
        asyncHttpClient.close();
    }
}
