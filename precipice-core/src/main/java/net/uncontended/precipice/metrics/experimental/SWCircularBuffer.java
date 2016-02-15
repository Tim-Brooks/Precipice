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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.metrics.CircularBuffer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Unstable and still in development. At this time, {@link CircularBuffer} should be used.
 */
public class SWCircularBuffer<T> {

    private final Slot<T>[] buffer;
    private final int mask;
    private final int totalSlots;
    private final int millisecondsPerSlot;
    private final long startTime;
    private volatile int currentIndex = 0;
    private int currentAbsoluteIndex = 0;

    @SuppressWarnings("unchecked")
    public SWCircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit, long startTime, T object) {
        long millisecondsPerSlot = slotUnit.toMillis(resolution);

        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = currentMillisTime(startTime);
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.buffer = (Slot<T>[]) new Object[arraySlot];

        for (int i = 0; i < totalSlots; ++i) {
            buffer[i] = new Slot<>(i);
        }
        buffer[0].set(0, object);
    }

    public T getSlot() {
        int index = currentIndex;
        return buffer[index].get();
    }

    // TODO: Make sure this logic works.
    public void put(long nanoTime, T object) {
        long currentTime = currentMillisTime(nanoTime);
        int absoluteSlot = currentAbsoluteSlot(currentTime);

        if (absoluteSlot != currentAbsoluteIndex) {
            int relativeSlot = absoluteSlot & mask;
            buffer[relativeSlot].set(absoluteSlot, object);
            currentIndex = relativeSlot;

            int upperBound = Math.min(absoluteSlot, currentAbsoluteIndex + totalSlots - 1);
            for (int i = currentAbsoluteIndex + 1; i < upperBound; ++i) {
                buffer[i & mask] = null;
            }

            currentAbsoluteIndex = absoluteSlot;
        }
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

    private static class Slot<T> {
        private static final long LONG_ZERO = (long) Integer.MAX_VALUE + 1;
        private T alwaysActiveObject;
        private T objectOne;
        private T objectTwo;
        private volatile long absoluteSlotAndFlag;

        private Slot(long absoluteSlot) {
            absoluteSlotAndFlag = absoluteSlot;
        }

        private void set(int absoluteSlot, T object) {
            long absoluteSlotAndFlag = this.absoluteSlotAndFlag;
            if (LONG_ZERO > absoluteSlotAndFlag) {
                objectTwo = object;
                alwaysActiveObject = object;
                this.absoluteSlotAndFlag = LONG_ZERO + absoluteSlot;
            } else {
                objectOne = object;
                alwaysActiveObject = object;
                this.absoluteSlotAndFlag = absoluteSlot;
            }
        }

        private T get() {
            return alwaysActiveObject;
        }

        private T getIfLive(int absoluteIndex) {
            for (; ; ) {
                T localOne = objectOne;
                T localTwo = objectTwo;
                long absoluteSlotAndFlag = this.absoluteSlotAndFlag;
                if (LONG_ZERO > absoluteSlotAndFlag) {
                    if (absoluteSlotAndFlag == absoluteIndex) {
                        return localOne;
                    } else {
                        return null;
                    }
                } else {
                    if (absoluteSlotAndFlag - LONG_ZERO == absoluteIndex) {
                        return localTwo;
                    } else {
                        return null;
                    }
                }
            }
        }
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
                    Slot<T> slot = buffer[relativeSlot];
                    T object = slot.getIfLive(currentIndex);
                    if (object != null) {
                        return object;
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
