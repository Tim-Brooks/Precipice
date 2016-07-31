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

import net.uncontended.precipice.metrics.counts.WritableCounts;
import net.uncontended.precipice.metrics.latency.WritableLatency;
import net.uncontended.precipice.time.Clock;

public class GuardRailBuilder<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final GuardRailProperties<Result, Rejected> properties = new GuardRailProperties<>();


    public GuardRailBuilder<Result, Rejected> addBackPressure(BackPressure<Rejected> backPressure) {
        properties.backPressureList.put(Integer.toString(properties.backPressureList.size()), backPressure);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addBackPressure(String name, BackPressure<Rejected> backPressure) {
        properties.backPressureList.put(name, backPressure);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> name(String name) {
        properties.name = name;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addResultMetrics(WritableCounts<Result> resultMetrics) {
        properties.resultMetrics.put(Integer.toString(properties.resultMetrics.size()), resultMetrics);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addResultMetrics(String name, WritableCounts<Result> resultMetrics) {
        properties.resultMetrics.put(name, resultMetrics);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addRejectedMetrics(WritableCounts<Rejected> rejectedMetrics) {
        properties.rejectedMetrics.put(Integer.toString(properties.rejectedMetrics.size()), rejectedMetrics);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addRejectedMetrics(String name, WritableCounts<Rejected> rejectedMetrics) {
        properties.rejectedMetrics.put(name, rejectedMetrics);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addResultLatency(WritableLatency<Result> resultLatency) {
        properties.resultLatency.put(Integer.toString(properties.resultLatency.size()), resultLatency);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> addResultLatency(String name, WritableLatency<Result> resultLatency) {
        properties.resultLatency.put(name, resultLatency);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> clock(Clock clock) {
        properties.clock = clock;
        return this;
    }

    public GuardRail<Result, Rejected> build() {
        if (properties.name == null) {
            throw new IllegalArgumentException();
        } else if (properties.resultMetrics.isEmpty()) {
            throw new IllegalArgumentException();
        } else if (properties.rejectedMetrics.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return GuardRail.create(properties);
    }
}
