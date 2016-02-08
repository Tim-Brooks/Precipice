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

package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.Result;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class GRProperties<Res extends Enum<Res> & Result, Rejected extends Enum<Rejected>> {

    private final Class<Res> resultType;
    private final Class<Rejected> rejectedType;
    private BPCountMetrics<Res> resultMetrics;
    private BPCountMetrics<Rejected> rejectedMetrics;
    private LatencyMetrics<Res> latencyMetrics;
    private Clock clock = new SystemTime();

    public GRProperties(Class<Res> resultType, Class<Rejected> rejectedType) {
        this.resultType = resultType;
        this.rejectedType = rejectedType;
        resultMetrics = new BPCountMetrics<>(resultType);
        rejectedMetrics = new BPCountMetrics<>(rejectedType);
        latencyMetrics = new IntervalLatencyMetrics<>(resultType);
    }

    public GRProperties<Res, Rejected> resultMetrics(BPCountMetrics<Res> resultMetrics) {
        this.resultMetrics = resultMetrics;
        return this;
    }

    public BPCountMetrics<Res> resultMetrics() {
        return resultMetrics;
    }


    public GRProperties<Res, Rejected> latencyMetrics(LatencyMetrics<Res> latencyMetrics) {
        this.latencyMetrics = latencyMetrics;
        return this;
    }

    public LatencyMetrics<Res> latencyMetrics() {
        return latencyMetrics;
    }

    public Clock clock() {
        return clock;
    }

    public void clock(Clock clock) {
        this.clock = clock;
    }
}
