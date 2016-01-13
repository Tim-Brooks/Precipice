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


package net.uncontended.precipice.utils;

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.SuperImpl;
import net.uncontended.precipice.metrics.ActionMetrics;

public class MetricCallback implements PrecipiceFunction<SuperImpl, Void> {

    private final ActionMetrics<SuperImpl> metrics;

    public MetricCallback(ActionMetrics<SuperImpl> metrics) {
        this.metrics = metrics;
    }

    @Override
    public void apply(SuperImpl status, Void argument) {
        metrics.incrementMetricCount(status);
    }
}
