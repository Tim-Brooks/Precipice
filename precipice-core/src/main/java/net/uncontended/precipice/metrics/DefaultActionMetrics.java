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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.utils.SystemTime;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultActionMetrics implements ActionMetrics {

    private final CircularBuffer<Slot> buffer;
    private final SystemTime systemTime;

    public DefaultActionMetrics() {
        this(3600, 1, TimeUnit.SECONDS);
    }

    public DefaultActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public DefaultActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit, SystemTime systemTime) {
        this.systemTime = systemTime;
        long millisecondsPerSlot = slotUnit.toMillis(resolution);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        } else if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }

        this.buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, systemTime.nanoTime());
    }

    @Override
    public void incrementMetricCount(Metric metric) {
        incrementMetricCount(metric, systemTime.nanoTime());
    }

    @Override
    public void incrementMetricCount(Metric metric, long nanoTime) {
        incrementMetricAndRecordLatency(metric, -1L, nanoTime);
    }

    @Override
    public void incrementMetricAndRecordLatency(Metric metric, long nanoLatency, long nanoTime) {
        Slot currentSlot = buffer.getSlot(nanoTime);
        if (currentSlot == null) {
            currentSlot = buffer.putOrGet(nanoTime, new Slot());
        }
        currentSlot.incrementMetric(metric);
        recordLatency(nanoLatency, currentSlot);
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCountForTimePeriod(metric, timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<Slot> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime);

        long count = 0;
        for (Slot slot : slots) {
            if (slot != null) {
                count = count + slot.getMetric(metric).longValue();
            }
        }
        return count;
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit) {
        return healthSnapshot(timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<Slot> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime);

        long total = 0;
        long failures = 0;
        long rejections = 0;
        for (Slot slot : slots) {
            if (slot != null) {
                long successes = slot.getMetric(Metric.SUCCESS).longValue();
                long errors = slot.getMetric(Metric.ERROR).longValue();
                long timeouts = slot.getMetric(Metric.TIMEOUT).longValue();
                long maxConcurrency = slot.getMetric(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED).longValue();
                long circuitOpen = slot.getMetric(Metric.CIRCUIT_OPEN).longValue();
                long queueFull = slot.getMetric(Metric.QUEUE_FULL).longValue();
                long slotTotal = successes + errors + timeouts + maxConcurrency + circuitOpen + queueFull;

                total = total + slotTotal;
                failures = failures + errors + timeouts;
                rejections = rejections + circuitOpen + queueFull + maxConcurrency;
            }
        }
        return new HealthSnapshot(total, failures, rejections);
    }

    @Override
    public Map<Object, Object> snapshot(long timePeriod, TimeUnit timeUnit) {
        return Snapshot.generate(buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, systemTime.nanoTime()));

    }

    private static void recordLatency(long nanoDuration, Slot slot) {
        if (nanoDuration != -1) {
            slot.recordLatency(nanoDuration);
        }
    }
}
