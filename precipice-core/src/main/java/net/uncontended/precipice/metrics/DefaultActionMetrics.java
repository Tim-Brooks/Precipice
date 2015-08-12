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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class DefaultActionMetrics implements ActionMetrics {

    private final AtomicReferenceArray<Slot> metrics;
    private final SystemTime systemTime;
    private final int mask;
    private final int totalSlots;
    private final int millisecondsPerSlot;
    private final long startTime;

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


        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = currentMillisTime(systemTime.nanoTime());
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.metrics = new AtomicReferenceArray<>(arraySlot);

        for (int i = 0; i < arraySlot; ++i) {
            metrics.set(i, new Slot(i));
        }
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
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;
        Slot slot = metrics.get(relativeSlot);

        if (slot.getAbsoluteSlot() == absoluteSlot) {
            slot.incrementMetric(metric);
            recordLatency(nanoLatency, slot);
        } else {
            for (; ; ) {
                slot = metrics.get(relativeSlot);
                if (slot.getAbsoluteSlot() == absoluteSlot) {
                    slot.incrementMetric(metric);
                    recordLatency(nanoLatency, slot);
                    break;
                } else {
                    Slot newSlot = new Slot(absoluteSlot);
                    if (metrics.compareAndSet(relativeSlot, slot, newSlot)) {
                        newSlot.incrementMetric(metric);
                        recordLatency(nanoLatency, slot);
                        break;
                    }
                }
            }
        }
    }

    private void recordLatency(long nanoDuration, Slot slot) {
        if (nanoDuration != -1) {
            slot.recordLatency(nanoDuration);
        }
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCountForTimePeriod(metric, timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        int slots = convertToSlots(timePeriod, timeUnit);
        long currentTime = currentMillisTime(nanoTime);

        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int startSlot = 1 + absoluteSlot - slots;
        int adjustedStartSlot = startSlot >= 0 ? startSlot : 0;

        long count = 0;
        for (int i = adjustedStartSlot; i <= absoluteSlot; ++i) {
            int relativeSlot = i & mask;
            Slot slot = metrics.get(relativeSlot);
            if (slot.getAbsoluteSlot() == i) {
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
        int slots = convertToSlots(timePeriod, timeUnit);
        Slot[] slotArray = collectActiveSlots(slots, nanoTime);

        long total = 0;
        long failures = 0;
        long rejections = 0;
        for (Slot slot : slotArray) {
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
        int slots = convertToSlots(timePeriod, timeUnit);
        return Snapshot.generate(collectActiveSlots(slots, systemTime.nanoTime()));

    }

    private Slot[] collectActiveSlots(int slots, long nanoTime) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int startSlot = 1 + absoluteSlot - slots;
        int adjustedStartSlot = startSlot >= 0 ? startSlot : 0;

        Slot[] slotArray = new Slot[slots];
        int j = 0;
        for (int i = adjustedStartSlot; i <= absoluteSlot; ++i) {
            int relativeSlot = i & mask;
            Slot slot = metrics.get(relativeSlot);
            if (slot.getAbsoluteSlot() == i) {
                slotArray[j] = slot;
            }
            ++j;
        }
        return slotArray;
    }

    private long currentMillisTime(long nanoTime) {
        return TimeUnit.NANOSECONDS.toMillis(nanoTime);
    }

    private int currentAbsoluteSlot(long currentTime) {
        return ((int) (currentTime - startTime)) / millisecondsPerSlot;
    }

    private int convertToSlots(long timePeriod, TimeUnit timeUnit) {
        long longSlots = timeUnit.toMillis(timePeriod) / millisecondsPerSlot;

        if (longSlots > totalSlots) {
            String message = String.format("Slots greater than slots tracked: [Tracked: %s, Argument: %s]",
                    totalSlots, longSlots);
            throw new IllegalArgumentException(message);
        } else if (longSlots <= 0) {
            String message = String.format("Slots must be greater than 0. [Argument: %s]", longSlots);
            throw new IllegalArgumentException(message);
        }
        return (int) longSlots;
    }

    private int nextPositivePowerOfTwo(int slotsToTrack) {
        return 1 << (32 - Integer.numberOfLeadingZeros(slotsToTrack - 1));
    }

}
