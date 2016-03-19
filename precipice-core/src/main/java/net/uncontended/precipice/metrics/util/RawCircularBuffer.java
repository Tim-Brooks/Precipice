/*
 * Copyright 2015 Timothy Brooks
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
 */

package net.uncontended.precipice.metrics.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RawCircularBuffer<T> {
    private final AtomicReferenceArray<Slot<T>> buffer;
    private final int mask;
    private final int totalSlots;
    private final int millisecondsPerSlot;
    private final long startTime;

    public RawCircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, System.nanoTime());
    }

    public RawCircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit, long startTime) {
        long millisecondsPerSlot = slotUnit.toMillis(resolution);

        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = currentMillisTime(startTime);
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.buffer = new AtomicReferenceArray<>(arraySlot);
        initiateBuffer();
    }

    public T getSlot(long nanoTime) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;
        Slot<T> slot = buffer.get(relativeSlot);

        if (slot.absoluteSlot == absoluteSlot) {
            return slot.object;
        } else {
            return null;
        }
    }

    public void put(long nanoTime, T object) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;
        buffer.set(relativeSlot, new Slot<>(absoluteSlot, object));
    }

    public T putOrGet(long nanoTime, T object) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;

        for (; ; ) {
            Slot<T> slot = buffer.get(relativeSlot);
            if (slot.absoluteSlot == absoluteSlot) {
                return slot.object;
            } else {
                Slot<T> newSlot = new Slot<>(absoluteSlot, object);
                if (buffer.compareAndSet(relativeSlot, slot, newSlot)) {
                    return newSlot.object;
                }
            }
        }
    }

    public Iterable<Slot<T>> collectActiveSlotsForTimePeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        int slots = convertToSlots(timePeriod, timeUnit);
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int startSlot = 1 + absoluteSlot - slots;
        int adjustedStartSlot = startSlot >= 0 ? startSlot : 0;
        return new SlotView(adjustedStartSlot, absoluteSlot);
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

    private void initiateBuffer() {
        for (int i = 0; i < totalSlots; ++i) {
            buffer.set(i, new Slot<T>(i, null));
        }
    }

    public static class Slot<T> {
        public final T object;
        public final long absoluteSlot;

        private Slot(long absoluteSlot, T object) {
            this.absoluteSlot = absoluteSlot;
            this.object = object;
        }
    }

    // Essentially what I need is an iterator that also iterates the slots. I need to know, based upon the slot, the
    // start and end time of a slot. This suggests that the slots should be persistent. And not "newed" up every
    // time we do a switch. This will make the concurrency logic a little more complicated it seems.

    private class SlotView implements Iterable<Slot<T>> {

        private final int maxIndex;
        private int index;

        private SlotView(int index, int maxIndex) {
            this.index = index;
            this.maxIndex = maxIndex;
        }

        @Override
        public Iterator<Slot<T>> iterator() {
            return new Iterator<Slot<T>>() {
                @Override
                public boolean hasNext() {
                    return index <= maxIndex;
                }

                @Override
                public Slot<T> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    int currentIndex = index++;
                    int relativeSlot = currentIndex & mask;
                    Slot<T> slot = buffer.get(relativeSlot);
                    if (slot.absoluteSlot == currentIndex) {
                        return slot;
                    } else {
                        // TODO: Remove null option
                        return null;
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
