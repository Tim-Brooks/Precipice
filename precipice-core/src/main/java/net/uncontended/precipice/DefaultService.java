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
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultService extends AbstractService implements MultiService {

    private final ExecutorService service;
    private final RunService runService;
    private final DefaultAsyncService submissionService;

    public DefaultService(String name, ExecutorService service, ServiceProperties properties) {
        this(name, service, properties, new AtomicBoolean(false));
    }

    private DefaultService(String name, ExecutorService service, ServiceProperties properties,
                           AtomicBoolean isShutdown) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore(), isShutdown);
        this.service = service;
        this.runService = new DefaultRunService(name, properties, isShutdown);
        this.submissionService = new DefaultAsyncService(name, service, properties, isShutdown);
    }

    @Override
    public <T> PrecipiceFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        return submissionService.submit(action, millisTimeout);
    }

    @Override
    public <T> void complete(ResilientAction<T> action, PrecipicePromise<T> promise, long millisTimeout) {
        submissionService.complete(action, promise, millisTimeout);
    }

    @Override
    public <T> T run(ResilientAction<T> action) throws Exception {
        return runService.run(action);
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);
        service.shutdown();
    }
}
