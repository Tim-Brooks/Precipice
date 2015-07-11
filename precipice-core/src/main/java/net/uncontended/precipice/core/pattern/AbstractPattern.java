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

package net.uncontended.precipice.core.pattern;

import net.uncontended.precipice.core.ResilientCallback;
import net.uncontended.precipice.core.concurrent.ResilientPromise;
import net.uncontended.precipice.core.metrics.ActionMetrics;
import net.uncontended.precipice.core.metrics.Metric;

public abstract class AbstractPattern<C> implements Pattern<C> {

    protected final ActionMetrics metrics;

    public AbstractPattern(ActionMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public ActionMetrics getActionMetrics() {
        return metrics;
    }

    protected static class MetricsCallback<T> implements ResilientCallback<T> {

        private final ActionMetrics metrics;
        private final ResilientCallback<T> callback;

        public MetricsCallback(ActionMetrics metrics, ResilientCallback<T> callback) {
            this.metrics = metrics;
            this.callback = callback;
        }

        @Override
        public void run(ResilientPromise<T> resultPromise) {
            metrics.incrementMetricCount(Metric.statusToMetric(resultPromise.getStatus()));
            if (callback != null) {
                callback.run(resultPromise);
            }
        }
    }
}
