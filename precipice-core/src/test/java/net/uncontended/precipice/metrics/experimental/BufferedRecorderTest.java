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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class BufferedRecorderTest {

    @Mock
    private Clock clock;

    private long currentValue = 0L;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.nanoTime()).thenReturn(0L);
    }

    @Test
    public void testThing() {
        LongWrapper total = new LongWrapper();
        total.value = -1;
        MetricRecorder<LongWrapper> metricRecorder = new MetricRecorder<>(total);
        BufferedRecorder<LongWrapper> buffered = new BufferedRecorder<>(metricRecorder, longAdderAllocator(), 4);
        buffered.advance(1L);
        System.out.println(buffered.current());
        buffered.advance(2L);
        System.out.println(buffered.current());
        buffered.advance(3L);
        System.out.println(buffered.current());
        buffered.advance(4L);
        System.out.println(buffered.current());
        buffered.advance(4L);
        System.out.println(buffered.current());

        System.out.println("\nIterate\n");

        IntervalIterator<LongWrapper> intervals = buffered.intervals(10L);
        while(intervals.hasNext()) {
            System.out.println(intervals.next());
        }


    }

    private NewAllocator<LongWrapper> longAdderAllocator() {
        return new NewAllocator<LongWrapper>() {
            @Override
            public LongWrapper allocateNew() {
                LongWrapper longWrapper = new LongWrapper();
                longWrapper.value = currentValue++;
                return longWrapper;
            }
        };
    }

    private class LongWrapper implements Resettable {

        private long value;

        @Override
        public void reset() {
            value = currentValue++;
        }

        @Override
        public String toString() {
            return "LongWrapper{" +
                    "value=" + value +
                    '}';
        }
    }
}
