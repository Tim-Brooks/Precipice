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

import net.uncontended.precipice.metrics.Metric;
import net.uncontended.precipice.timeout.ActionTimeoutException;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultRunService extends AbstractService implements RunService {

    public DefaultRunService(String name, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
    }

    public DefaultRunService(String name, ServiceProperties properties, AtomicBoolean isShutdown) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore(), isShutdown);
    }

    @Override
    public <T> T run(final ResilientAction<T> action) throws Exception {
        acquirePermitOrRejectIfActionNotAllowed();
        try {
            T result = action.run();
            metricsAndBreakerFeedback(Status.SUCCESS);
            return result;
        } catch (ActionTimeoutException e) {
            metricsAndBreakerFeedback(Status.TIMEOUT);
            throw e;
        } catch (Exception e) {
            metricsAndBreakerFeedback(Status.ERROR);
            throw e;
        } finally {
            semaphore.releasePermit();
        }
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);

    }

    private void metricsAndBreakerFeedback(Status status) {
        long nanoTime = System.nanoTime();
        actionMetrics.incrementMetricCount(Metric.statusToMetric(status), nanoTime);
        circuitBreaker.informBreakerOfResult(status == Status.SUCCESS, nanoTime);
    }
}
