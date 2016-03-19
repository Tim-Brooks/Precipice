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

    public Iterable<T> activeValuesForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return activeValuesForTimePeriod(timePeriod, timeUnit, nanoTime, null);
    }

    public Iterable<T> activeValuesForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime, T dead) {
        return activeValues(convertToSlots(timePeriod, timeUnit), nanoTime, dead);
    }

    public Iterable<T> activeValues(long slots, long nanoTime, T dead) {
        long absoluteSlot = currentAbsoluteSlot(nanoTime);
        long startSlot = 1 + absoluteSlot - slots;
        long adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
        return new SlotView(adjustedStartSlot, absoluteSlot, dead);
    }

    private int toRelative(long absoluteSlot) {
        return (int) (absoluteSlot & mask);
    }

    private long convertToSlots(long timePeriod, TimeUnit timeUnit) {
        long slotCount = timeUnit.toNanos(timePeriod) / nanosPerSlot;

        if (slotCount > totalSlots) {
            String message = String.format("Slots greater than slots tracked: [Tracked: %s, Argument: %s]", totalSlots, slotCount);
            throw new IllegalArgumentException(message);
        }
        if (slotCount <= 0) {
            String message = String.format("Slots must be greater than 0. [Argument: %s]", slotCount);
            throw new IllegalArgumentException(message);
        }
        return slotCount;
    }

    private long currentAbsoluteSlot(long nanoTime) {
        return (nanoTime - startNanos) / nanosPerSlot;
    }

    private static int nextPositivePowerOfTwo(int slotsToTrack) {
        return 1 << 32 - Integer.numberOfLeadingZeros(slotsToTrack - 1);
    }

    public static class Slot<T> {
        public final T object;
        public final long absoluteSlot;

        private Slot(long absoluteSlot, T object) {
            this.absoluteSlot = absoluteSlot;
            this.object = object;
        }
    }

    private class SlotView implements Iterable<T>, Iterator<T> {

        private final T dead;
        private final long maxIndex;
        private long index;

        private SlotView(long index, long maxIndex, T dead) {
            this.index = index;
            this.maxIndex = maxIndex;
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
        public Iterator<T> iterator() {
            return this;
        }
    }
}
