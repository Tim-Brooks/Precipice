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

import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.concurrent.ExecutorSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.Metric;

public class DefaultRunService extends AbstractService implements RunService {

    public DefaultRunService(int concurrencyLevel) {
        this(concurrencyLevel, new DefaultActionMetrics());
    }

    public DefaultRunService(int concurrencyLevel, ActionMetrics actionMetrics) {
        this(concurrencyLevel, actionMetrics, new DefaultCircuitBreaker(actionMetrics, new
                BreakerConfigBuilder().build()));
    }

    public DefaultRunService(int concurrencyLevel, CircuitBreaker breaker) {
        this(concurrencyLevel, new DefaultActionMetrics(), breaker);
    }

    public DefaultRunService(int concurrencyLevel, ActionMetrics actionMetrics, CircuitBreaker
            circuitBreaker) {
        super(circuitBreaker, actionMetrics, new ExecutorSemaphore(concurrencyLevel));
    }

    @Override
    public <T> T run(final ResilientAction<T> action) throws Exception {
        acquirePermitOrRejectIfActionNotAllowed();
        try {
            T result = action.run();
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.SUCCESS));
            return result;
        } catch (ActionTimeoutException e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.TIMEOUT));
            throw e;
        } catch (Exception e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.ERROR));
            throw e;
        } finally {
            semaphore.releasePermit();
        }
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);

    }
}
