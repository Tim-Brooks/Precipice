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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ExampleMetrics {

    private final MetricRegistry metrics;
    private final ActionMetrics actionMetrics;
    private final AtomicLong lastUpdateTimestamp = new AtomicLong(0);
    private volatile Map<Object, Object> currentMetrics;

    public ExampleMetrics(MetricRegistry metrics, String name, ActionMetrics actionMetrics) {
        this.metrics = metrics;
        this.actionMetrics = actionMetrics;

        metrics.register(name + " Total", new Gauge<Long>() {

            @Override
            public Long getValue() {
                return getMetrics(Snapshot.TOTAL);
            }
        });

        metrics.register(name + " Successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getMetrics(Snapshot.SUCCESSES);
            }
        });

        metrics.register(name + " Errors", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getMetrics(Snapshot.ERRORS);
            }
        });

        metrics.register(name + " Timeouts", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getMetrics(Snapshot.TIMEOUTS);
            }
        });

        metrics.register(name + " Rejections - Circuit Open", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getMetrics(Snapshot.CIRCUIT_OPEN);
            }
        });

        metrics.register(name + " Rejections - Max Concurrency", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getMetrics(Snapshot.MAX_CONCURRENCY);
            }
        });
    }

    private Long getMetrics(String metric) {
        long currentTime = System.currentTimeMillis();
        long lastUpdateTime = lastUpdateTimestamp.get();
        if (currentTime - 1000 > lastUpdateTime && lastUpdateTimestamp.compareAndSet(lastUpdateTime, currentTime)) {
            currentMetrics = actionMetrics.snapshot(1, TimeUnit.SECONDS);
        }

        return (Long) currentMetrics.get(metric);
    }


}
