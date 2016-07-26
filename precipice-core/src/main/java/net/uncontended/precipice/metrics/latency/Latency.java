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

package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.tools.Allocator;

import java.util.concurrent.TimeUnit;

public final class Latency {

    private Latency() {
    }

    public static <T extends Enum<T>> Allocator<PartitionedLatency<T>> concurrentHDRHistogram(Class<T> clazz) {
        return concurrentHDRHistogram(clazz, TimeUnit.HOURS.toNanos(1), 2);
    }

    private static <T extends Enum<T>> Allocator<PartitionedLatency<T>> concurrentHDRHistogram(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
        return new ConcurrentHDRHistogramFactory<>(clazz, highestTrackableValue, numberOfSignificantValueDigits);
    }

    private static class ConcurrentHDRHistogramFactory<T extends Enum<T>> implements Allocator<PartitionedLatency<T>> {

        private final Class<T> clazz;
        private final long highestTrackableValue;
        private final int numberOfSignificantValueDigits;

        public ConcurrentHDRHistogramFactory(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
            this.clazz = clazz;
            this.highestTrackableValue = highestTrackableValue;
            this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        }

        @Override
        public PartitionedLatency<T> allocateNew() {
            return new ConcurrentHistogram<>(clazz, highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    public static <T extends Enum<T>> Allocator<PartitionedLatency<T>> atomicHDRHistogram(Class<T> clazz) {
        return atomicHDRHistogram(clazz, TimeUnit.HOURS.toNanos(1), 2);
    }

    public static <T extends Enum<T>> Allocator<PartitionedLatency<T>> atomicHDRHistogram(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
        return new AtomicHDRHistogramFactory<>(clazz, highestTrackableValue, numberOfSignificantValueDigits);
    }

    private static class AtomicHDRHistogramFactory<T extends Enum<T>> implements Allocator<PartitionedLatency<T>> {

        private final Class<T> clazz;
        private final long highestTrackableValue;
        private final int numberOfSignificantValueDigits;

        public AtomicHDRHistogramFactory(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
            this.clazz = clazz;
            this.highestTrackableValue = highestTrackableValue;
            this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        }

        @Override
        public PartitionedLatency<T> allocateNew() {
            return new AtomicHistogram<>(clazz, highestTrackableValue, numberOfSignificantValueDigits);
        }
    }
}
