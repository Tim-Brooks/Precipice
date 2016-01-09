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

public class SWCircularBuffer<T> {

    private final T[] buffer;
    private final int mask;
    private final int totalSlots;
    private final int millisecondsPerSlot;
    private final long startTime;
    private volatile int currentIndex = 0;

    public SWCircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, System.nanoTime());
    }

    @SuppressWarnings("unchecked")
    public SWCircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit, long startTime) {
        long millisecondsPerSlot = slotUnit.toMillis(resolution);

        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = currentMillisTime(startTime);
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.buffer = (T[]) new Object[arraySlot];
    }

    public T getSlot() {
        int index = currentIndex;
        return buffer[index];
    }

    public void put(long nanoTime, T object) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;

        // TODO: this is flawed. It assumes that the tick will never be delayed for a full traversal
        if (relativeSlot != currentIndex) {
            buffer[relativeSlot] = object;
        }

        currentIndex = relativeSlot;
    }

    public Iterable<T> collectActiveSlotsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, null);
    }

    public Iterable<T> collectActiveSlotsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime, T dead) {
        int slots = convertToSlots(timePeriod, timeUnit);
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int startSlot = 1 + absoluteSlot - slots;
        int adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
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

    private int currentAbsoluteSlot(long currentTime) {
        return (int) (currentTime - startTime) / millisecondsPerSlot;
    }

    private static long currentMillisTime(long nanoTime) {
        return TimeUnit.NANOSECONDS.toMillis(nanoTime);
    }

    private static int nextPositivePowerOfTwo(int slotsToTrack) {
        return 1 << 32 - Integer.numberOfLeadingZeros(slotsToTrack - 1);
    }

    private class SlotView implements Iterable<T> {

        private final T dead;
        private final int maxIndex;
        private int index;

        private SlotView(int index, int maxIndex, T dead) {
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
                    int currentIndex = index++;
                    int relativeSlot = currentIndex & mask;
                    T slot = buffer[relativeSlot];
                    if (slot != null) {
                        return slot;
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
