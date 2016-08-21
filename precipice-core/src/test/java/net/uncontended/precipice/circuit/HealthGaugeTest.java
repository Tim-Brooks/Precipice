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

package net.uncontended.precipice.circuit;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.counts.LongAdderCounter;
import net.uncontended.precipice.metrics.counts.PartitionedCount;
import net.uncontended.precipice.result.SimpleResult;
import net.uncontended.precipice.result.TimeoutableResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HealthGaugeTest {

    @Mock
    private Rolling<PartitionedCount<SimpleResult>> rolling1;
    @Mock
    private Rolling<PartitionedCount<TimeoutableResult>> rolling2;
    @Mock
    private IntervalIterator<PartitionedCount<SimpleResult>> intervals1;
    @Mock
    private IntervalIterator<PartitionedCount<TimeoutableResult>> intervals2;

    private PartitionedCount<SimpleResult> current1;
    private PartitionedCount<TimeoutableResult> current2;

    private HealthGauge healthGauge;

    @Before
    public void setUp() {
        current1 = new LongAdderCounter<>(SimpleResult.class);
        current2 = new LongAdderCounter<>(TimeoutableResult.class);
        MockitoAnnotations.initMocks(this);

        when(rolling1.current()).thenReturn(current1);
        when(rolling2.current()).thenReturn(current2);

        healthGauge = new HealthGauge();
        healthGauge.add(rolling1);
        healthGauge.add(rolling2);
    }

    @Test
    public void requestedTimeIsDelegatedToIntervals() {
        when(rolling1.intervals(30L)).thenReturn(intervals1);
        when(rolling2.intervals(30L)).thenReturn(intervals2);

        healthGauge.getHealth(10L, TimeUnit.NANOSECONDS, 30L);

        verify(intervals1).limit(10L, TimeUnit.NANOSECONDS);
        verify(intervals2).limit(10L, TimeUnit.NANOSECONDS);
    }

    @Test
    public void snapshotReflectsMetrics() {
        PartitionedCount<SimpleResult> count1 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count1, 5, 7);
        PartitionedCount<SimpleResult> count2 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count2, 10, 3);
        PartitionedCount<SimpleResult> count3 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count3, 2, 0);
        PartitionedCount<TimeoutableResult> count4 = new LongAdderCounter<>(TimeoutableResult.class);
        addTimeoutableResults(count4, 18, 2, 7);
        PartitionedCount<TimeoutableResult> count5 = new LongAdderCounter<>(TimeoutableResult.class);
        addTimeoutableResults(count5, 9, 1, 0);


        when(rolling1.intervals(30L)).thenReturn(intervals1);
        when(rolling2.intervals(30L)).thenReturn(intervals2);
        when(intervals1.hasNext()).thenReturn(true, true, true, false);
        when(intervals1.next()).thenReturn(count1, count2, count3);
        when(intervals2.hasNext()).thenReturn(true, true, false);
        when(intervals2.next()).thenReturn(count4, count5);

        HealthSnapshot health = healthGauge.getHealth(10L, TimeUnit.NANOSECONDS, 30L);

        assertEquals(64, health.total);
        assertEquals(20, health.failures);
        assertEquals(100 * 20 / 64, health.failurePercentage);
    }

    @Test
    public void oldValuesAreRefreshed() {
        PartitionedCount<SimpleResult> count1 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count1, 5, 7);
        PartitionedCount<SimpleResult> count2 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count2, 10, 3);
        PartitionedCount<SimpleResult> count3 = new LongAdderCounter<>(SimpleResult.class);
        addSimpleResults(count3, 2, 0);
        PartitionedCount<TimeoutableResult> count4 = new LongAdderCounter<>(TimeoutableResult.class);
        addTimeoutableResults(count4, 18, 2, 7);
        PartitionedCount<TimeoutableResult> count5 = new LongAdderCounter<>(TimeoutableResult.class);
        addTimeoutableResults(count5, 9, 1, 0);

        when(rolling1.intervals(30L)).thenReturn(intervals1);
        when(rolling2.intervals(30L)).thenReturn(intervals2);
        when(intervals1.hasNext()).thenReturn(true, true, false, true, false);
        when(intervals1.next()).thenReturn(count1, count2, count3);
        when(intervals2.hasNext()).thenReturn(true, false, true, false);
        when(intervals2.next()).thenReturn(count4, count5);

        healthGauge.getHealth(10L, TimeUnit.NANOSECONDS, 30L);
        HealthSnapshot health = healthGauge.getHealth(10L, TimeUnit.NANOSECONDS, 30L);

        assertEquals(12, health.total);
        assertEquals(1, health.failures);
        assertEquals(100 * 1 / 12, health.failurePercentage);

    }

    private static void addSimpleResults(PartitionedCount<SimpleResult> counts, long successes, long errors) {
        counts.add(SimpleResult.SUCCESS, successes);
        counts.add(SimpleResult.ERROR, errors);

    }

    private static void addTimeoutableResults(PartitionedCount<TimeoutableResult> counts, long successes, long errors, long timeouts) {
        counts.add(TimeoutableResult.SUCCESS, successes);
        counts.add(TimeoutableResult.ERROR, errors);
        counts.add(TimeoutableResult.TIMEOUT, timeouts);
    }


}
