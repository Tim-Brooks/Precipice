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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import net.uncontended.precipice.*;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.timeout.ActionTimeoutException;

import java.util.concurrent.TimeoutException;


public class HttpAsyncService extends AbstractService implements AsyncService {

    private final AsyncHttpClient client;
    private final ActionMetrics<Status> metrics;

    public HttpAsyncService(String name, ServiceProperties properties, AsyncHttpClient client) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        this.metrics = (ActionMetrics<Status>) properties.actionMetrics();
        this.client = client;
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(final ResilientAction<T> action, long millisTimeout) {
        acquirePermitOrGetRejectedReason();
        final Eventual<Status, T> eventual = new Eventual<>();

        final ServiceRequest<T> asyncRequest = (ServiceRequest<T>) action;
        client.executeRequest(asyncRequest.getRequest(), new AsyncCompletionHandler<Void>() {
            @Override
            public Void onCompleted(Response response) throws Exception {
                asyncRequest.setResponse(response);
                try {
                    T result = asyncRequest.run();
                    metrics.incrementMetricCount(Status.SUCCESS);
                    eventual.complete(Status.SUCCESS, result);
                } catch (ActionTimeoutException e) {
                    metrics.incrementMetricCount(Status.TIMEOUT);
                    eventual.completeExceptionally(Status.TIMEOUT, e);
                } catch (Exception e) {
                    metrics.incrementMetricCount(Status.ERROR);
                    eventual.completeExceptionally(Status.ERROR, e);
                }
                semaphore.releasePermit();
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    metrics.incrementMetricCount(Status.TIMEOUT);
                    eventual.completeExceptionally(Status.TIMEOUT, t);
                } else {
                    metrics.incrementMetricCount(Status.ERROR);
                    eventual.completeExceptionally(Status.ERROR, t);
                }
                semaphore.releasePermit();
            }
        });
        return eventual;
    }


    @Override
    public void complete(ResilientAction action, PrecipicePromise promise, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }
}
