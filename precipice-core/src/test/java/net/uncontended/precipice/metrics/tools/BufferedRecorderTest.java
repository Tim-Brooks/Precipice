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

import net.uncontended.precipice.metrics.Resettable;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class BufferedRecorderTest {

    @Mock
    private Clock clock;

    private final LongWrapper total = new LongWrapper();

    private long currentValue = 0L;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.nanoTime()).thenReturn(0L);
    }

//    @Test
//    public void gettersDelegateToRecorder() {
//        MetricRecorder recorder = mock(MetricRecorder.class);
//        BufferedRecorder<LongWrapper> buffer = new BufferedRecorder<>(recorder, longAdderAllocator(), 4, clock);
//
//        LongWrapper current = new LongWrapper();
//        when(recorder.current()).thenReturn(current);
//
//        assertSame(current, buffer.get());
//
//        when(recorder.current(100L)).thenReturn(current);
//
//        assertSame(current, buffer.get(100L));
//    }

//    @Test
//    public void testAdvancesSlotsAsExpected() {
//        BufferedRecorder<LongWrapper> buffered = new BufferedRecorder<>(longAdderAllocator(), 2, clock);
//
//        assertEquals(0, buffered.get().value);
//        assertEquals(0, buffered.get().pastValue);
//        buffered.advance(1L);
//        assertEquals(2, buffered.get().value);
//        assertEquals(2, buffered.get().pastValue);
//        buffered.advance(2L);
//        assertEquals(3, buffered.get().value);
//        assertEquals(1, buffered.get().pastValue);
//        buffered.advance(3L);
//        assertEquals(4, buffered.get().value);
//        assertEquals(0, buffered.get().pastValue);
//        buffered.advance(4L);
//        assertEquals(5, buffered.get().value);
//        assertEquals(2, buffered.get().pastValue);
//        buffered.advance(5L);
//        assertEquals(6, buffered.get().value);
//        assertEquals(3, buffered.get().pastValue);
//    }

//    @Test
//    public void testIterator() {
//        BufferedRecorder<LongWrapper> buffered = new BufferedRecorder<>(longAdderAllocator(), 2, clock);
//
//        IntervalIterator<LongWrapper> intervals = buffered.intervals(10L);
//
//        // TODO: Decide on intervals.start and end logical representations
//
//        while (intervals.hasNext()) {
//            assertEquals(0, intervals.next().value);
//            assertEquals(-10, intervals.intervalStart());
//            assertEquals(0, intervals.intervalEnd());
//        }
//    }

    private Allocator<LongWrapper> longAdderAllocator() {
        return new Allocator<LongWrapper>() {
            @Override
            public LongWrapper allocateNew() {
                LongWrapper longWrapper = new LongWrapper();
                longWrapper.value = currentValue++;
                longWrapper.pastValue = longWrapper.value;
                return longWrapper;
            }
        };
    }

    private class LongWrapper implements Resettable {

        private long pastValue;
        private long value;

        @Override
        public void reset() {
            pastValue = value;
            value = currentValue++;
        }

        @Override
        public String toString() {
            return "LongWrapper{" +
                    "pastValue=" + pastValue +
                    ", value=" + value +
                    '}';
        }
    }
}
