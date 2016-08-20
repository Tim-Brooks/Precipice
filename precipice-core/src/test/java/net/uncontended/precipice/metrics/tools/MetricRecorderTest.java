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

import net.uncontended.precipice.metrics.counts.LongAdderCounter;
import net.uncontended.precipice.rejected.Unrejectable;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class MetricRecorderTest {

    @Mock
    private LongAdderCounter<Unrejectable> active;
    @Mock
    private LongAdderCounter<Unrejectable> inactive;
    @Mock
    private Clock clock;
    @Mock
    private FlipControl<LongAdderCounter<Unrejectable>> flipControl;

    private MetricRecorder<LongAdderCounter<Unrejectable>> recorder;
    private long startTime = 100;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.nanoTime()).thenReturn(startTime);
        recorder = new MetricRecorder<>(active, inactive, flipControl, clock);

        verify(flipControl).flip(active);
        assertEquals(startTime, recorder.activeIntervalStart());
    }

    @Test
    public void activeIntervalIsFromFlipControl() {
        when(flipControl.active()).thenReturn(active);
        assertEquals(active, recorder.activeInterval());
    }

    @Test
    public void captureReturnsActiveAndReusesInactive() {
        when(flipControl.flip(inactive)).thenReturn(active);
        LongAdderCounter<Unrejectable> counter = recorder.captureInterval(200L);
        verify(inactive).reset();
        assertEquals(active, counter);
        assertEquals(200L, recorder.activeIntervalStart());

        when(flipControl.flip(active)).thenReturn(inactive);
        counter = recorder.captureInterval(300L);
        verify(active).reset();
        assertEquals(inactive, counter);
        assertEquals(300L, recorder.activeIntervalStart());
    }

    @Test
    public void captureReturnsUsesCounterIfPassed() {
        LongAdderCounter<Unrejectable> newCounter = mock(LongAdderCounter.class);
        recorder.captureInterval(newCounter, 200L);
        verify(flipControl).flip(newCounter);
    }

    @Test
    public void startAndEndDelegatesToFlipControl() {
        when(flipControl.startRecord()).thenReturn(20L);
        long permit = recorder.startRecord();
        assertEquals(20L, permit);
        verify(flipControl).startRecord();

        recorder.endRecord(permit);
        verify(flipControl).endRecord(permit);
    }
}
