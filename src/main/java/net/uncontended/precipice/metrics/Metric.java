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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.Status;

/**
 * Created by timbrooks on 6/1/15.
 */
public enum Metric {
    SUCCESS(false),
    ERROR(false),
    TIMEOUT(false),
    CIRCUIT_OPEN(true),
    QUEUE_FULL(true),
    MAX_CONCURRENCY_LEVEL_EXCEEDED(true);

    private final boolean actionRejected;

    Metric(boolean actionRejected) {
        this.actionRejected = actionRejected;
    }

    public boolean actionRejected() {
        return actionRejected;
    }

    public static Metric statusToMetric(Status status) {
        switch (status) {
            case SUCCESS:
                return Metric.SUCCESS;
            case ERROR:
                return Metric.ERROR;
            case TIMEOUT:
                return Metric.TIMEOUT;
            default:
                throw new RuntimeException("Cannot convert Status to Metric: " + status);
        }
    }
}
