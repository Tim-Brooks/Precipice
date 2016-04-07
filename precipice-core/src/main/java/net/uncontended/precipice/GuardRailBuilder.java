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

import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.NoOpLatency;
import net.uncontended.precipice.metrics.WritableCountMetrics;
import net.uncontended.precipice.time.Clock;

public class GuardRailBuilder<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private GuardRailProperties<Result, Rejected> properties = new GuardRailProperties<>();


    public GuardRailBuilder<Result, Rejected> addBackPressure(BackPressure<Rejected> backPressure) {
        this.properties.backPressureList.add(backPressure);
        return this;
    }

    public GuardRailBuilder<Result, Rejected> name(String name) {
        this.properties.name = name;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> resultMetrics(WritableCountMetrics<Result> resultMetrics) {
        this.properties.resultMetrics = resultMetrics;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> rejectedMetrics(WritableCountMetrics<Rejected> rejectedMetrics) {
        this.properties.rejectedMetrics = rejectedMetrics;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> resultLatency(LatencyMetrics<Result> resultLatency) {
        this.properties.resultLatency = resultLatency;
        return this;
    }

    public GuardRailBuilder<Result, Rejected> clock(Clock clock) {
        this.properties.clock = clock;
        return this;
    }

    public GuardRail<Result, Rejected> build() {
        if (properties.name == null) {
            throw new IllegalArgumentException();
        } else if (properties.resultMetrics == null) {
            throw new IllegalArgumentException();
        } else if (properties.rejectedMetrics == null) {
            throw new IllegalArgumentException();
        }

        if (properties.resultLatency == null) {
            properties.resultLatency = new NoOpLatency<>(properties.resultMetrics.getMetricClazz());
        }

        return GuardRail.create(properties);
    }
}
