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

/**
 * Created by timbrooks on 6/1/15.
 */
public class Slot {

    private final LongAdder successes = new LongAdder();
    private final LongAdder errors = new LongAdder();
    private final LongAdder timeouts = new LongAdder();
    private final LongAdder circuitOpen = new LongAdder();
    private final LongAdder queueFull = new LongAdder();
    private final LongAdder maxConcurrencyExceeded = new LongAdder();
    private final long second;

    public Slot(long second) {
        this.second = second;
    }

    public void incrementMetric(Metric metric) {
        switch (metric) {
            case SUCCESS:
                successes.increment();
                break;
            case ERROR:
                errors.increment();
                break;
            case TIMEOUT:
                timeouts.increment();
                break;
            case CIRCUIT_OPEN:
                circuitOpen.increment();
                break;
            case QUEUE_FULL:
                queueFull.increment();
                break;
            case MAX_CONCURRENCY_LEVEL_EXCEEDED:
                maxConcurrencyExceeded.increment();
                break;
            default:
                throw new RuntimeException("Unknown metric: " + metric);
        }
    }

    public LongAdder getMetric(Metric metric) {
        switch (metric) {
            case SUCCESS:
                return successes;
            case ERROR:
                return errors;
            case TIMEOUT:
                return timeouts;
            case CIRCUIT_OPEN:
                return circuitOpen;
            case QUEUE_FULL:
                return queueFull;
            case MAX_CONCURRENCY_LEVEL_EXCEEDED:
                return maxConcurrencyExceeded;
            default:
                throw new RuntimeException("Unknown metric: " + metric);
        }
    }

    public long getAbsoluteSlot() {
        return second;
    }

    @Override
    public String toString() {
        return "Slot{" +
                "successes=" + successes +
                ", errors=" + errors +
                ", timeouts=" + timeouts +
                ", circuitOpen=" + circuitOpen +
                ", queueFull=" + queueFull +
                ", maxConcurrencyExceeded=" + maxConcurrencyExceeded +
                ", second=" + second +
                '}';
    }
}
