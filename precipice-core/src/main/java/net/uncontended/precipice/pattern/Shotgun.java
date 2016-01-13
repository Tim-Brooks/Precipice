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

import net.uncontended.precipice.AsyncService;
import net.uncontended.precipice.RejectedActionException;
import net.uncontended.precipice.RejectionReason;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.DefaultActionMetrics;

import java.util.Map;

public class Shotgun<C> extends AbstractPattern<C> implements AsyncPattern<C> {

    private final AsyncService[] services;
    private final ShotgunStrategy strategy;
    private final C[] contexts;

    public Shotgun(Map<AsyncService, C> executorToContext, int submissionCount) {
        this(executorToContext, submissionCount, new ShotgunStrategy(executorToContext.size(), submissionCount));
    }

    @SuppressWarnings("unchecked")
    public Shotgun(Map<AsyncService, C> executorToContext, int submissionCount, ShotgunStrategy strategy) {
        super(new DefaultActionMetrics(Status.class));
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > executorToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be greater than the number of services " +
                    "provided.");
        }

        services = new AsyncService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<AsyncService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }

        this.strategy = strategy;
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        Eventual<Status, T> promise = new Eventual<>();
        final int[] servicesToTry = strategy.executorIndices();

        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            try {
                ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);
                actionWithContext.context = contexts[serviceIndex];
                AsyncService service = services[serviceIndex];
                service.complete(actionWithContext, promise, millisTimeout);
                ++submittedCount;
            } catch (RejectedActionException e) {
            }
            if (submittedCount == strategy.getSubmissionCount()) {
                break;
            }
        }
        if (submittedCount == 0) {
            throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
        }
        return promise;
    }

    @Override
    public <T> void complete(ResilientPatternAction<T, C> action, PrecipicePromise<Status, T> promise, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        for (AsyncService service : services) {
            service.shutdown();
        }

    }
}
