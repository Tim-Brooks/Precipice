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

import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.Precipice;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.factories.Asynchronous;
import net.uncontended.precipice.rejected.Rejected;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class HttpAsyncService implements Precipice<HTTPStatus, Rejected> {

    private final AsyncHttpClient client;
    private final GuardRail<HTTPStatus, Rejected> guardRail;
    private final String url;

    public HttpAsyncService(GuardRail<HTTPStatus, Rejected> guardRail, String url, AsyncHttpClient client) {
        this.guardRail = guardRail;
        this.url = url;
        this.client = client;
    }

    public PrecipiceFuture<HTTPStatus, Response> submit(RequestBuilder request) {
        request.setUrl(url);

        final PrecipicePromise<HTTPStatus, Response> promise = Asynchronous.acquirePermitsAndPromise(guardRail, 1L);
        client.executeRequest(request, new AsyncCompletionHandler<Void>() {
            @Override
            public Void onCompleted(Response response) throws Exception {
                int httpStatus = response.getStatusCode();
                if (httpStatus < 200 || httpStatus > 299) {
                    promise.complete(HTTPStatus.NON_200, response);
                } else {
                    promise.complete(HTTPStatus.STATUS_200, response);
                }
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    promise.completeExceptionally(HTTPStatus.TIMEOUT, t);
                } else {
                    promise.completeExceptionally(HTTPStatus.ERROR, t);
                }
            }
        });
        return promise.future();
    }

    @Override
    public GuardRail<HTTPStatus, Rejected> guardRail() {
        return guardRail;
    }


    public void shutdown(boolean shutdownClient) throws IOException {
        if (shutdownClient) {
            client.close();
        }
    }
}
