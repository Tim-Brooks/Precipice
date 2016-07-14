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

package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class BufferedRecorder<T> extends Recorder<T> {

    private final int bufferSize;
    private final AtomicReferenceArray<Interval<T>> buffer;
    private final Recorder<T> recorder;
    private final Clock clock;
    private final int mask;
    private volatile long currentIndex;
    private Interval<T> inactive;

    public BufferedRecorder(Recorder<T> recorder, int bufferSize) {
        this(recorder, bufferSize, new SystemTime());
    }

    public BufferedRecorder(Recorder<T> recorder, int bufferSize, Clock clock) {
        this.recorder = recorder;
        this.clock = clock;
        this.bufferSize = bufferSize;
        bufferSize = nextPositivePowerOfTwo(bufferSize);
        this.mask = bufferSize - 1;
        this.buffer = new AtomicReferenceArray<>(bufferSize);
        inactive = new Interval<>(null, -1, -1);
    }

    public void init(Allocator<T> allocator) {
        int length = buffer.length();
        long nanoTime = clock.nanoTime();
        for (int i = 0; i < length; ++i) {
            int difference = length - i;
            long endTime = nanoTime - difference;
            buffer.set(i, new Interval<>(allocator.allocateNew(), endTime - 1, endTime));
        }

        Interval<T> interval = buffer.get(0);
        interval.startNanos = nanoTime;
        interval.isInit = true;
        recorder.flip(interval.object);
    }

    public T get() {
        return recorder.active();
    }

    public IntervalIterator<T> intervals() {
        return intervals(clock.nanoTime());
    }

    public IntervalIterator<T> intervals(long nanoTime) {
        BufferedIterator bufferedIterator = new BufferedIterator();
        bufferedIterator.reset(nanoTime);
        return bufferedIterator;
    }

    private static int nextPositivePowerOfTwo(int bufferSize) {
        return 1 << 32 - Integer.numberOfLeadingZeros(bufferSize - 1);
    }

    @Override
    public long startRecord() {
        return recorder.startRecord();
    }

    @Override
    public void endRecord(long permit) {
        recorder.endRecord(permit);
    }

    @Override
    public T flip(T newValue) {
        return advance(newValue, clock.nanoTime());
    }

    public T advance(T newValue, long nanoTime) {

        long oldIndex = this.currentIndex;
        long newIndex = oldIndex + 1;
        int newRelativeIndex = (int) newIndex & mask;
        Interval<T> reuse = buffer.get(newRelativeIndex);
        inactive.startNanos = nanoTime;
        inactive.startNanos = nanoTime - 1;
        inactive.object = newValue;
        buffer.set(newRelativeIndex, inactive);
        inactive.isInit = true;
        recorder.flip(inactive.object);
        Interval<T> closingInterval = buffer.get((int) oldIndex & mask);
        closingInterval.endNanos = nanoTime;
        this.currentIndex = newIndex;
        T returned = reuse.object;
        reuse.object = null;
        inactive = reuse;
        return returned;
    }

    private static class Interval<T> {
        private T object;
        private long startNanos;
        private long endNanos;
        private boolean isInit = false;

        private Interval(T object, long startNanos, long endNanos) {
            this.object = object;
            this.startNanos = startNanos;
            this.endNanos = endNanos;
        }

        @Override
        public String toString() {
            return "Interval{" +
                    "object=" + object +
                    ", startNanos=" + startNanos +
                    ", endNanos=" + endNanos +
                    ", isInit=" + isInit +
                    '}';
        }
    }

    private class BufferedIterator implements IntervalIterator<T> {

        private long index;
        private long maxIndex;
        private Interval<T> value;
        private T dead = null;
        private long nanoTime;

        @Override
        public boolean hasNext() {
            long diff = maxIndex - index;
            return diff >= 0;
        }

        @Override
        public T next() {
            long absolute = index++;
            value = buffer.get((int) absolute & mask);
            if (nanoTime - value.startNanos < 0) {
                return dead;
            }
            return value.object;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public long intervalStart() {
            return value.startNanos - nanoTime;
        }

        @Override
        public long intervalEnd() {
            if (value.endNanos - value.startNanos >= 0) {
                return value.endNanos - nanoTime;
            } else {
                return 0L;
            }
        }

        @Override
        public IntervalIterator<T> limit(long duration, TimeUnit unit) {
            long limitTime = nanoTime - unit.toNanos(duration);
            long newIndex = index;
            while (maxIndex - index >= 0) {
                long absolute = newIndex++;
                if (buffer.get((int) absolute & mask).startNanos - limitTime <= 0) {
                    break;
                }
            }
            index = newIndex;
            return this;
        }

        @Override
        public IntervalIterator<T> reset(long nanoTime) {
            this.nanoTime = nanoTime;
            maxIndex = currentIndex;
            index = maxIndex - (bufferSize - 1);

            while (maxIndex - index >= 0) {
                if (buffer.get((int) index & mask).isInit) {
                    break;
                }
                ++index;
            }
            return this;
        }
    }
}
