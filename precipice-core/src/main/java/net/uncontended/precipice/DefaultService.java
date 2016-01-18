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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.threadpool.ThreadpoolService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class DefaultService extends AbstractService implements MultiService {

    private final ExecutorService service;
    private final RunService runService;
    private final ThreadpoolService threadpoolService;

    public DefaultService(String name, ExecutorService service, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        this.service = service;

        ControllerProperties<Status> controllerProperties = new ControllerProperties<>(Status.class);
        controllerProperties.actionMetrics((ActionMetrics<Status>) properties.actionMetrics());
        controllerProperties.latencyMetrics((LatencyMetrics<Status>) properties.latencyMetrics());
        Controller<Status> controller = new Controller<>(name, controllerProperties);

        runService = new DefaultRunService(controller);
        threadpoolService = new ThreadpoolService(service, controller);
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(final ResilientAction<T> action, long millisTimeout) {
        return threadpoolService.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return action.run();
            }
        }, millisTimeout);
    }

    @Override
    public <T> void complete(final ResilientAction<T> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        threadpoolService.complete(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return action.run();
            }
        }, promise, millisTimeout);
    }

    @Override
    public Controller<Status> controller() {
        return null;
    }

    @Override
    public <T> T run(ResilientAction<T> action) throws Exception {
        return runService.run(action);
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        runService.shutdown();
        threadpoolService.controller().shutdown();
        service.shutdown();
    }
}
