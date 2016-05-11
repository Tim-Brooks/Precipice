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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.metrics.tools.MetricRecorder;
import net.uncontended.precipice.metrics.tools.Recorder;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MetricRecorderTest {

    private final Object total = new AtomicLong();
    private final Object current1 = new AtomicLong();
    private final Object current2 = new AtomicLong();

    private MetricRecorder<Object> longRecorder = new MetricRecorder<>(total, current1);

    @Test
    public void recorderReturnsCorrectLongs() {
        assertSame(longRecorder.current(), current1);
        assertSame(longRecorder.total(), total);
    }

    @Test
    public void recorderCanFlip() {
        assertSame(current1, longRecorder.current());
        assertSame(total, longRecorder.total());

        Object old = longRecorder.flip(current2);

        assertSame(current1, old);
        assertSame(current2, longRecorder.current());
        assertSame(total, longRecorder.total());
    }

    @Test
    public void recorderIsUsedToCoordinateFlip() {
        Recorder<Object> recorder = mock(Recorder.class);
        longRecorder = new MetricRecorder<>(total, current1, recorder);

        longRecorder.flip(current2);

        verify(recorder).flip(current2);
    }

}
