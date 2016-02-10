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

import net.uncontended.precipice.BackPressure;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.NoOpLatencyMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.ArrayList;
import java.util.List;

public class GuardRailBuilder<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private String name;
    private BPTotalCountMetrics<Result> resultMetrics;
    private BPTotalCountMetrics<Rejected> rejectedMetrics;
    private List<BackPressure<Rejected>> backPressureList = new ArrayList<>();
    private LatencyMetrics<Result> resultLatency = new NoOpLatencyMetrics<>();
    private Clock clock = new SystemTime();

    public GuardRailBuilder<Result, Rejected> addBackPressure(BackPressure<Rejected> backPressure) {
        this.backPressureList.add(backPressure);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> name(String name) {
        this.name = name;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> resultMetrics(BPTotalCountMetrics<Result> resultMetrics) {
        this.resultMetrics = resultMetrics;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> rejectedMetrics(BPTotalCountMetrics<Rejected> rejectedMetrics) {
        this.rejectedMetrics = rejectedMetrics;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> resultLatency(LatencyMetrics<Result> resultLatency) {
        this.resultLatency = resultLatency;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public GuardRail<Result, Rejected> build() {
        if (name == null) {
            throw new IllegalArgumentException();
        } else if (resultMetrics == null) {
            throw new IllegalArgumentException();
        } else if (rejectedMetrics == null) {
            throw new IllegalArgumentException();
        }

        return new GuardRail<>(name, resultMetrics, rejectedMetrics, resultLatency, backPressureList, clock);
    }
}
