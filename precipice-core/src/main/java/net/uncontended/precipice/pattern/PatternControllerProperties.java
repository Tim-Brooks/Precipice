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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.Result;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;

public class PatternControllerProperties<T extends Enum<T> & Result> {

    private final Class<T> type;
    private ActionMetrics<T> metrics;
    private LatencyMetrics<T> latencyMetrics;

    public PatternControllerProperties(Class<T> type) {
        this.type = type;
        metrics = new DefaultActionMetrics<T>(type);
        latencyMetrics = new IntervalLatencyMetrics<>(type);
    }

    public PatternControllerProperties<T> actionMetrics(ActionMetrics<T> metrics) {
        this.metrics = metrics;
        return this;
    }

    public ActionMetrics<T> actionMetrics() {
        return metrics;
    }

    public PatternControllerProperties<T> latencyMetrics(LatencyMetrics<T> latencyMetrics) {
        this.latencyMetrics = latencyMetrics;
        return this;
    }

    public LatencyMetrics<T> latencyMetrics() {
        return latencyMetrics;
    }
}
