/*
 * Copyright 2016 Timothy Brooks
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

public enum SuperImpl implements SuperStatusInterface {
    SUCCESS(false, false, true),
    ERROR(true, false, true),
    TIMEOUT(true, false, true),
    CIRCUIT_OPEN(false, true),
    QUEUE_FULL(false, true),
    MAX_CONCURRENCY_LEVEL_EXCEEDED(false, true),
    ALL_SERVICES_REJECTED(false, true),
    CANCELLED(false, false),
    PENDING(false, false);

    private final boolean isFailed;
    private final boolean isRejected;
    private final boolean trackLatency;

    SuperImpl(boolean isFailed, boolean isRejected) {
        this(isFailed, isRejected, false);
    }

    SuperImpl(boolean isFailed, boolean isRejected, boolean trackLatency) {
        this.isFailed = isFailed;
        this.isRejected = isRejected;
        this.trackLatency = trackLatency;
    }


    @Override
    public boolean isRejected() {
        return isRejected;
    }

    @Override
    public boolean isFailure() {
        return isFailed;
    }

    @Override
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    @Override
    public boolean trackLatency() {
        return trackLatency;
    }

    @Override
    public boolean trackMetrics() {
        return true;
    }
}
