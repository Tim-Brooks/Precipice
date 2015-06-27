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

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.concurrent.ResilientPromise;
import net.uncontended.precipice.metrics.ActionMetrics;

public class DefaultCompletionService extends AbstractService implements CompletionService {


    public DefaultCompletionService(CircuitBreaker circuitBreaker, ActionMetrics actionMetrics,
                                    PrecipiceSemaphore semaphore) {
        super(circuitBreaker, actionMetrics, semaphore);
    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, ResilientPromise<T> promise, long millisTimeout) {

    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, ResilientPromise<T> promise,
                                      ResilientCallback<T> callback, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);
    }
}
