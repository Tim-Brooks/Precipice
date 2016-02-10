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
    private final int millisecondsPerSlot;
    private final long startTime;

    public CircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, System.nanoTime());
    }

    public CircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit, long startTime) {
        long millisecondsPerSlot = slotUnit.toMillis(resolution);

        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = currentMillisTime(startTime);
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.buffer = new AtomicReferenceArray<>(arraySlot);
    }

    public T getSlot(long nanoTime) {
        long currentTime = currentMillisTime(nanoTime);
        long absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = (int) absoluteSlot & mask;
        Slot<T> slot = buffer.get(relativeSlot);

        if (slot != null && slot.absoluteSlot == absoluteSlot) {
            return slot.object;
        } else {
            return null;
        }
    }

    public T putOrGet(long nanoTime, T object) {
        long currentTime = currentMillisTime(nanoTime);
        long absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = (int) absoluteSlot & mask;

        for (; ; ) {
            Slot<T> slot = buffer.get(relativeSlot);
            // TODO: Ensure this handles backwards in time.
            if (slot != null && slot.absoluteSlot >= absoluteSlot) {
                return slot.object;
            } else {
                Slot<T> newSlot = new Slot<>(absoluteSlot, object);
                if (buffer.compareAndSet(relativeSlot, slot, newSlot)) {
                    return newSlot.object;
                }
            }
        }
    }

    public Iterable<T> collectActiveSlotsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, null);
    }

    public Iterable<T> collectActiveSlotsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime, T dead) {
        int slots = convertToSlots(timePeriod, timeUnit);
        long currentTime = currentMillisTime(nanoTime);
        long absoluteSlot = currentAbsoluteSlot(currentTime);
        long startSlot = 1 + absoluteSlot - slots;
        long adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
        return new SlotView(adjustedStartSlot, absoluteSlot, dead);
    }

    private int convertToSlots(long timePeriod, TimeUnit timeUnit) {
        long longSlots = timeUnit.toMillis(timePeriod) / millisecondsPerSlot;

        if (longSlots > totalSlots) {
            String message = String.format("Slots greater than slots tracked: [Tracked: %s, Argument: %s]",
                    totalSlots, longSlots);
            throw new IllegalArgumentException(message);
        }
        if (longSlots <= 0) {
            String message = String.format("Slots must be greater than 0. [Argument: %s]", longSlots);
            throw new IllegalArgumentException(message);
        }
        return (int) longSlots;
    }

    private long currentAbsoluteSlot(long currentTime) {
        return (currentTime - startTime) / millisecondsPerSlot;
    }

    private static long currentMillisTime(long nanoTime) {
        return TimeUnit.NANOSECONDS.toMillis(nanoTime);
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

    private class SlotView implements Iterable<T> {

        private final T dead;
        private final long maxIndex;
        private long index;

        private SlotView(long index, long maxIndex, T dead) {
            this.index = index;
            this.maxIndex = maxIndex;
            this.dead = dead;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return index <= maxIndex;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    long currentIndex = index++;
                    int relativeSlot = (int) currentIndex & mask;
                    Slot<T> slot = buffer.get(relativeSlot);
                    if (slot != null && slot.absoluteSlot == currentIndex) {
                        return slot.object;
                    } else {
                        return dead;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }
}
