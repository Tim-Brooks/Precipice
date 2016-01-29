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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.Controllable;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ThreadPoolLoadBalancer<C> implements Controllable<Status> {

    private final ThreadPoolService[] services;
    private final Map<Controllable<Status>, C> contexts;
    private final Balancer<Status, ThreadPoolService> balancer;


    @SuppressWarnings("unchecked")
    public ThreadPoolLoadBalancer(Map<? extends ThreadPoolService, C> executorToContext, Balancer<Status, ThreadPoolService> balancer) {
        if (executorToContext.isEmpty()) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        services = new ThreadPoolService[executorToContext.size()];
        contexts = new HashMap<>(executorToContext.size());
        int i = 0;
        for (Map.Entry<? extends ThreadPoolService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts.put(entry.getKey(), entry.getValue());
            ++i;
        }
        this.balancer = balancer;
    }

    public <T> PrecipiceFuture<Status, T> submit(PatternAction<T, C> action, long millisTimeout) {
        PatternPair<ThreadPoolService, PrecipicePromise<Status, T>> pair = balancer.promisePair();
        internalComplete(action, pair, millisTimeout);
        return pair.completable.future();
    }

    public <T> void complete(PatternAction<T, C> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PatternPair<ThreadPoolService, PrecipicePromise<Status, T>> pair = balancer.promisePair(promise);
        internalComplete(action, pair, millisTimeout);
    }

    public void shutdown() {
        for (ThreadPoolService e : services) {
            e.shutdown();
        }
    }

    private <T> void internalComplete(final PatternAction<T, C> action,
                                      PatternPair<ThreadPoolService, PrecipicePromise<Status, T>> pair,
                                      long millisTimeout) {
        ThreadPoolService service = pair.controllable;
        final C context = contexts.get(service);
        Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return action.run(context);
            }
        };
        // TODO: Add executor service getter
        service.bypassBackPressureAndComplete(callable, pair.completable, millisTimeout);
    }

    @Override
    public Controller<Status> controller() {
        return balancer.controller();
    }
}
