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

package net.uncontended.precipice.core.samples.http;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import net.uncontended.precipice.core.AbstractService;
import net.uncontended.precipice.core.ResilientAction;
import net.uncontended.precipice.core.ServiceProperties;
import net.uncontended.precipice.core.SubmissionService;
import net.uncontended.precipice.core.concurrent.Eventual;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.Promise;

import java.util.concurrent.TimeoutException;


public class HttpAsyncService extends AbstractService implements SubmissionService {

    private final AsyncHttpClient client;

    public HttpAsyncService(String name, ServiceProperties properties, AsyncHttpClient client) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
        this.client = client;
    }

    @Override
    public <T> PrecipiceFuture<T> submit(final ResilientAction<T> action, long millisTimeout) {
        final Eventual<T> eventual = new Eventual<>();
        // Update Metrics
        final ServiceRequest<T> asyncRequest = (ServiceRequest<T>) action;
        client.executeRequest(asyncRequest.getRequest(), new AsyncCompletionHandler<Void>() {
            @Override
            public Void onCompleted(Response response) throws Exception {
                asyncRequest.setResponse(response);
                T result = asyncRequest.run();
                eventual.complete(result);
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    eventual.completeWithTimeout();
                } else {
                    eventual.completeExceptionally(t);
                }
            }
        });
        return eventual;
    }


    @Override
    public void complete(ResilientAction action, Promise promise, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        isShutdown.set(true);
    }
}
