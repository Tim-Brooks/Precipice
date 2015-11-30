/*
 * Copyright 2015 Timothy Brooks
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

package net.uncontended.precipice.samples.exploratory;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import net.uncontended.precipice.AbstractService;
import net.uncontended.precipice.Service;
import net.uncontended.precipice.ServiceProperties;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.metrics.Metric;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class ActionService extends AbstractService implements Service {
    private final AsyncHttpClient client;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ActionService(String name, AsyncHttpClient client, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        this.client = client;
    }

    public PrecipiceFuture<Response> execute(Request request) throws Exception {
        acquirePermitOrRejectIfActionNotAllowed();
        final Eventual<Response> promise = new Eventual<>();

        client.executeRequest(request, new AsyncCompletionHandler<Void>() {
            @Override
            public Void onCompleted(Response response) throws Exception {
                promise.complete(response);
                semaphore.releasePermit();
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    actionMetrics.incrementMetricCount(Metric.TIMEOUT);
                    promise.completeWithTimeout();
                } else {
                    actionMetrics.incrementMetricCount(Metric.ERROR);
                    promise.completeExceptionally(t);
                }
                semaphore.releasePermit();
            }
        });

        return promise;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }
}
