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
package net.uncontended.precipice.core.pattern;

import net.uncontended.precipice.core.CompletionService;
import net.uncontended.precipice.core.RejectedActionException;
import net.uncontended.precipice.core.RejectionReason;
import net.uncontended.precipice.core.concurrent.Eventual;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.metrics.DefaultActionMetrics;

import java.util.Map;

public class Shotgun<C> extends AbstractPattern<C> implements SubmissionPattern<C> {

    private final CompletionService[] services;
    private final ShotgunStrategy strategy;
    private final C[] contexts;

    public Shotgun(Map<CompletionService, C> executorToContext, int submissionCount) {
        this(executorToContext, submissionCount, new ShotgunStrategy(executorToContext.size(), submissionCount));
    }

    @SuppressWarnings("unchecked")
    public Shotgun(Map<CompletionService, C> executorToContext, int submissionCount, ShotgunStrategy strategy) {
        super(new DefaultActionMetrics());
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > executorToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be greater than the number of services " +
                    "provided.");
        }

        services = new CompletionService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<CompletionService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }

        this.strategy = strategy;
    }

    @Override
    public <T> PrecipiceFuture<T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        Eventual<T> promise = new Eventual<>();
        final int[] servicesToTry = strategy.executorIndices();

        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            try {
                ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);
                actionWithContext.context = contexts[serviceIndex];
                CompletionService service = services[serviceIndex];
                service.submitAndComplete(actionWithContext, promise, millisTimeout);
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
    public void shutdown() {
        for (CompletionService service : services) {
            service.shutdown();
        }

    }
}
