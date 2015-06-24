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

import net.uncontended.precipice.concurrent.DefaultResilientPromise;
import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.concurrent.ResilientPromise;

import java.util.Map;

public class Shotgun<C> implements SubmissionPattern<C>, CompletionPattern<C> {

    private final MultiService[] services;
    private final ShotgunStrategy strategy;
    private final C[] contexts;

    @SuppressWarnings("unchecked")
    public Shotgun(Map<MultiService, C> executorToContext, int submissionCount) {
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > executorToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be fewer than number of services provided.");
        }

        services = new MultiService[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<MultiService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }

        this.strategy = new ShotgunStrategy(services.length, submissionCount);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        return submit(action, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientPatternAction<T, C> action, ResilientCallback<T> callback,
                                         long millisTimeout) {
        DefaultResilientPromise<T> promise = new DefaultResilientPromise<>();
        submitAndComplete(action, promise, callback, millisTimeout);
        return new ResilientFuture<>(promise);
    }

    @Override
    public <T> void submitAndComplete(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      long millisTimeout) {
        submitAndComplete(action, promise, null, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      ResilientCallback<T> callback, long millisTimeout) {
        final int[] servicesToTry = strategy.executorIndices();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            try {
                actionWithContext.context = contexts[serviceIndex];
                services[serviceIndex].submitAndComplete(actionWithContext, promise, callback, millisTimeout);
                ++submittedCount;
            } catch (RejectedActionException e) {
            }
            if (submittedCount == strategy.submissionCount) {
                break;
            }
        }
        if (submittedCount == 0) {
            throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
        }
    }

    @Override
    public void shutdown() {
        for (MultiService service : services) {
            service.shutdown();
        }

    }
}
