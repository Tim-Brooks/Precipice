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
import net.uncontended.precipice.metrics.ActionMetrics;

/**
 * Created by timbrooks on 6/23/15.
 */
public interface Service {
    long MAX_TIMEOUT_MILLIS = 1000 * 60 * 60 * 24;
    int MAX_CONCURRENCY_LEVEL = Integer.MAX_VALUE / 2;

    /**
     * Returns the {@link ActionMetrics} for this service.
     *
     * @return the metrics backing this service
     */
    ActionMetrics getActionMetrics();

    /**
     * Returns the {@link CircuitBreaker} for this service.
     *
     * @return the circuit breaker for this service
     */
    CircuitBreaker getCircuitBreaker();

    /**
     * Attempts to shutdown the service. Calls made to submit or run
     * after this call will throw a {@link RejectedActionException}. Implementations
     * may differ on if pending or executing actions are cancelled.
     */
    void shutdown();
}
