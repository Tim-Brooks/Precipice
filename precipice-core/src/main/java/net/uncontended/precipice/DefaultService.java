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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;

import java.util.concurrent.ExecutorService;

public class DefaultService extends AbstractService implements MultiService {

    private final ExecutorService service;
    private final RunService runService;
    private final DefaultAsyncService asyncService;

    public DefaultService(String name, ExecutorService service, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        this.service = service;
        runService = new DefaultRunService(name, properties);
        asyncService = new DefaultAsyncService(name, service, properties);
    }

    @Override
    public <T> PrecipiceFuture<SuperImpl, T> submit(ResilientAction<T> action, long millisTimeout) {
        return asyncService.submit(action, millisTimeout);
    }

    @Override
    public <T> void complete(ResilientAction<T> action, PrecipicePromise<SuperImpl, T> promise, long millisTimeout) {
        asyncService.complete(action, promise, millisTimeout);
    }

    @Override
    public <T> T run(ResilientAction<T> action) throws Exception {
        return runService.run(action);
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        runService.shutdown();
        asyncService.shutdown();
        service.shutdown();
    }
}
