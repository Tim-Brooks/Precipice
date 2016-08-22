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

package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.tools.Allocator;

public final class Counters {

    private Counters() {
    }

    public static <T extends Enum<T>> Allocator<PartitionedCount<T>> longAdder(Class<T> clazz) {
        return new LongAdderAllocator<>(clazz);
    }

    public static <T extends Enum<T>> Allocator<PartitionedCount<T>> atomicLong(Class<T> clazz) {
        return new AtomicLongAllocator<>(clazz);
    }

    public static <T extends Enum<T>> Allocator<PartitionedCount<T>> longCounter(Class<T> clazz) {
        return new LongAllocator<>(clazz);
    }

    private static class LongAdderAllocator<T extends Enum<T>> implements Allocator<PartitionedCount<T>> {

        private final Class<T> clazz;

        private LongAdderAllocator(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public PartitionedCount<T> allocateNew() {
            return new LongAdderCounter<>(clazz);
        }
    }

    private static class AtomicLongAllocator<T extends Enum<T>> implements Allocator<PartitionedCount<T>> {

        private final Class<T> clazz;

        private AtomicLongAllocator(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public PartitionedCount<T> allocateNew() {
            return new AtomicLongCounter<>(clazz);
        }
    }

    private static class LongAllocator<T extends Enum<T>> implements Allocator<PartitionedCount<T>> {

        private final Class<T> clazz;

        private LongAllocator(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public PartitionedCount<T> allocateNew() {
            return new LongCounter<>(clazz);
        }
    }
}
