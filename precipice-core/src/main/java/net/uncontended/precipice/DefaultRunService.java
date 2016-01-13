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

import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.timeout.ActionTimeoutException;

public class DefaultRunService extends AbstractService implements RunService {

    private final ActionMetrics<Status> actionMetrics;

    public DefaultRunService(String name, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        actionMetrics = (ActionMetrics<Status>) properties.actionMetrics();
    }

    @Override
    public <T> T run(final ResilientAction<T> action) throws Exception {
        RejectionReason rejectionReason = acquirePermitOrGetRejectedReason();
        if (rejectionReason != null) {
            handleRejectedReason(rejectionReason);
        }
        long nanoStart = System.nanoTime();
        try {
            T result = action.run();
            metricsAndBreakerFeedback(nanoStart, Status.SUCCESS);
            return result;
        } catch (ActionTimeoutException e) {
            metricsAndBreakerFeedback(nanoStart, Status.TIMEOUT);
            throw e;
        } catch (Exception e) {
            metricsAndBreakerFeedback(nanoStart, Status.ERROR);
            throw e;
        } finally {
            semaphore.releasePermit();
        }
    }

    @Override
    public void shutdown() {
        isShutdown = true;

    }

    private void handleRejectedReason(RejectionReason rejectionReason) {
        if (rejectionReason == RejectionReason.CIRCUIT_OPEN) {
            actionMetrics.incrementMetricCount(Status.CIRCUIT_OPEN);
        } else if (rejectionReason == RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED) {
            actionMetrics.incrementMetricCount(Status.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        }
        throw new RejectedActionException(rejectionReason);
    }

    private void metricsAndBreakerFeedback(long nanoStart, Status status) {
        long nanoTime = System.nanoTime();
        actionMetrics.incrementMetricCount(status, nanoTime);
        circuitBreaker.informBreakerOfResult(status == Status.SUCCESS, nanoTime);
        long latency = nanoTime - nanoStart;
        latencyMetrics.recordLatency(status, latency, nanoTime);
    }
}
