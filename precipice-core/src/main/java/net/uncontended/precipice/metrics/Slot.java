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

import net.uncontended.precipice.concurrent.LongAdder;
import org.HdrHistogram.Recorder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Slot {

    private final LongAdder[] metrics;
    private final Recorder recorder = new Recorder(1, TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES), 2);
    private final long absoluteSlot;

    public Slot(long absoluteSlot) {
        this.absoluteSlot = absoluteSlot;
        Metric[] metricValues = Metric.values();

        metrics = new LongAdder[metricValues.length];
        for (Metric metric : metricValues) {
            metrics[metric.ordinal()] = new LongAdder();
        }
    }

    public void incrementMetric(Metric metric) {
        metrics[metric.ordinal()].increment();
    }

    public LongAdder getMetric(Metric metric) {
        return metrics[metric.ordinal()];
    }

    public void recordLatency(long duration) {
        recorder.recordValue(duration);
    }

    public long getAbsoluteSlot() {
        return absoluteSlot;
    }

    @Override
    public String toString() {
        return "Slot{" +
                "metrics=" + Arrays.toString(metrics) +
                ", absoluteSlot=" + absoluteSlot +
                '}';
    }
}
