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

package net.uncontended.precipice.core;

import net.uncontended.precipice.core.timeout.ActionTimeoutException;
import net.uncontended.precipice.core.metrics.Metric;

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
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.SUCCESS));
            circuitBreaker.informBreakerOfResult(true);
            return result;
        } catch (ActionTimeoutException e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.TIMEOUT));
            circuitBreaker.informBreakerOfResult(false);
            throw e;
        } catch (Exception e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.ERROR));
            circuitBreaker.informBreakerOfResult(false);
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
