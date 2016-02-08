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
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.NoOpLatencyMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.ArrayList;
import java.util.List;

public class GuardRailBuilder<Res extends Enum<Res> & Result, Rejected extends Enum<Rejected>> {

    private String name;
    private BPTotalCountMetrics<Res> resultMetrics;
    private BPTotalCountMetrics<Rejected> rejectedMetrics;
    private List<BackPressure<Rejected>> backPressureList = new ArrayList<>();
    private LatencyMetrics<Res> resultLatency = new NoOpLatencyMetrics<>();
    private Clock clock = new SystemTime();

    public GuardRailBuilder addBackPressure(BackPressure<Rejected> backPressure) {
        this.backPressureList.add(backPressure);
        return this;
    }

    public GuardRailBuilder name(String name) {
        this.name = name;
        return this;
    }

    public GuardRailBuilder resultMetrics(BPTotalCountMetrics<Res> resultMetrics) {
        this.resultMetrics = resultMetrics;
        return this;
    }

    public GuardRailBuilder rejectedMetrics(BPTotalCountMetrics<Rejected> rejectedMetrics) {
        this.rejectedMetrics = rejectedMetrics;
        return this;
    }

    public GuardRailBuilder resultLatency(LatencyMetrics<Res> resultLatency) {
        this.resultLatency = resultLatency;
        return this;
    }

    public GuardRail<Res, Rejected> build() {
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
