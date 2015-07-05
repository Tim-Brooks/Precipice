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

    public DefaultRunService(ServiceProperties properties) {
        super(properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
    }

    public DefaultRunService(ServiceProperties properties, AtomicBoolean isShutdown) {
        super(properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore(), isShutdown);
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
