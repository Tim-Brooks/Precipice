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

package net.uncontended.precipice.example.bigger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.Snapshot;

import java.util.concurrent.TimeUnit;

public class ExampleMetrics {

    private final MetricRegistry metrics;

    public ExampleMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    public void addMetrics(String name, final ActionMetrics actionMetrics) {
        metrics.register(name + " Total", new Gauge<Long>() {

            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.TOTAL);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.SUCCESSES);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.ERRORS);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.TIMEOUTS);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.CIRCUIT_OPEN);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (Long) actionMetrics.snapshot(1, TimeUnit.SECONDS).get(Snapshot.MAX_CONCURRENCY);
            }
        });
    }


}
