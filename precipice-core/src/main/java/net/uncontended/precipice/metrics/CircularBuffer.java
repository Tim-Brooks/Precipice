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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CircularBuffer<T> {

    private final AtomicReferenceArray<Slot<T>> buffer;
    private final int mask;
    private final int totalSlots;
    private final long nanosPerSlot;
    private final long startNanos;

    public CircularBuffer(int slotsToTrack, long resolution, TimeUnit timeUnit) {
        this(slotsToTrack, resolution, timeUnit, System.nanoTime());
    }

    public CircularBuffer(int slotsToTrack, long resolution, TimeUnit timeUnit, long startNanos) {
        this.nanosPerSlot = timeUnit.toNanos(resolution);
        validateSlotSize(nanosPerSlot);

        this.startNanos = startNanos;
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.buffer = new AtomicReferenceArray<>(arraySlot);

        for (int i = 0; i < arraySlot; ++i) {
            this.buffer.set(i, new Slot<T>(-1, null));
        }
    }

    public T getSlot(long nanoTime) {
        long absoluteSlot = currentAbsoluteSlot(nanoTime);
        int relativeSlot = toRelative(absoluteSlot);
        Slot<T> slot = buffer.get(relativeSlot);

        if (slot != null && slot.absoluteSlot == absoluteSlot) {
            return slot.object;
        } else {
            return null;
        }
    }

    public T putOrGet(long nanoTime, T object) {
        long absoluteSlot = currentAbsoluteSlot(nanoTime);
        int relativeSlot = toRelative(absoluteSlot);

        for (; ; ) {
            Slot<T> slot = buffer.get(relativeSlot);
            if (slot.absoluteSlot == absoluteSlot) {
                return slot.object;
            } else if (slot.absoluteSlot > absoluteSlot) {
                return null;
            } else {
                Slot<T> newSlot = new Slot<>(absoluteSlot, object);
                if (buffer.compareAndSet(relativeSlot, slot, newSlot)) {
                    return newSlot.object;
                }
            }
        }
    }

    public Iterator<T> valuesForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return valuesForTimePeriod(timePeriod, timeUnit, nanoTime, null);
    }

    public Iterator<T> valuesForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime, T dead) {
        return values(convertToSlots(timePeriod, timeUnit), nanoTime, dead);
    }

    public Iterator<T> values(long slots, long nanoTime, T dead) {
        long diff = nanoTime - startNanos;
        long absoluteSlot = diff / nanosPerSlot;
        long startSlot = 1 + absoluteSlot - slots;
        long adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
        return new Intervals(adjustedStartSlot, absoluteSlot, -1, dead);
    }

    public IntervalIterator<T> intervalsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return intervalsForTimePeriod(timePeriod, timeUnit, nanoTime, null);
    }

    public IntervalIterator<T> intervalsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime, T dead) {
        return intervals(convertToSlots(timePeriod, timeUnit), nanoTime, dead);
    }

    public IntervalIterator<T> intervals(long slots, long nanoTime, T dead) {
        long diff = nanoTime - startNanos;
        long absoluteSlot = diff / nanosPerSlot;
        long startSlot = 1 + absoluteSlot - slots;
        long remainderNanos = diff % nanosPerSlot;
        long adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
        return new Intervals(adjustedStartSlot, absoluteSlot, remainderNanos, dead);
    }

    private int toRelative(long absoluteSlot) {
        return (int) (absoluteSlot & mask);
    }

    private long currentAbsoluteSlot(long nanoTime) {
        return (nanoTime - startNanos) / nanosPerSlot;
    }

    private long convertToSlots(long timePeriod, TimeUnit timeUnit) {
        long slotCount = timeUnit.toNanos(timePeriod) / nanosPerSlot;

        if (slotCount > totalSlots) {
            String message = String.format("Slots greater than slots tracked: [Tracked: %s, Argument: %s]", totalSlots, slotCount);
            throw new IllegalArgumentException(message);
        }
        if (slotCount <= 0) {
            String message = String.format("Time period must be greater than 0. Found: [%s timePeriod]", timePeriod);
            throw new IllegalArgumentException(message);
        }
        return slotCount;
    }

    private static void validateSlotSize(long nanosPerSlot) {
        if (nanosPerSlot < 0) {
            String message = "Nanoseconds per slot must be positive. Found: [%s nanoseconds]";
            throw new IllegalArgumentException(String.format(message, Integer.MAX_VALUE));
        }
        if (TimeUnit.MILLISECONDS.toNanos(100) > nanosPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s nanoseconds]. 100 " +
                    "milliseconds is the minimum resolution.", nanosPerSlot));
        }

    }

    private static int nextPositivePowerOfTwo(int slotsToTrack) {
        return 1 << 32 - Integer.numberOfLeadingZeros(slotsToTrack - 1);
    }

    private static class Slot<T> {
        private final T object;
        private final long absoluteSlot;

        private Slot(long absoluteSlot, T object) {
            this.absoluteSlot = absoluteSlot;
            this.object = object;
        }
    }

    private class Intervals implements IntervalIterator<T> {

        private final long remainderNanos;
        private final T dead;
        private final long maxIndex;
        private long index;

        private Intervals(long index, long maxIndex, long remainderNanos, T dead) {
            this.index = index;
            this.maxIndex = maxIndex;
            this.remainderNanos = remainderNanos;
            this.dead = dead;
        }

        @Override
        public boolean hasNext() {
            return index <= maxIndex;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            long absoluteSlot = index++;
            int relativeSlot = toRelative(absoluteSlot);
            Slot<T> slot = buffer.get(relativeSlot);
            if (slot.absoluteSlot == absoluteSlot && slot.object != null) {
                return slot.object;
            } else {
                return dead;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public long intervalStart() {
            long difference = maxIndex - index + 1;
            return -(remainderNanos + (difference * nanosPerSlot));
        }

        @Override
        public long intervalEnd() {
            long difference = maxIndex - index + 1;
            if (difference == 0) {
                return 0;
            } else {
                return -(remainderNanos + ((difference - 1) * nanosPerSlot));
            }
        }

        @Override
        public void limit(long duration, TimeUnit unit) {
            // TODO: Check logic
            long slots = convertToSlots(duration, unit);
            long startSlot = 1 + maxIndex - slots;
            index = startSlot >= 0 ? startSlot : 0;
        }
    }
}
