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

package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RollingCountTest {

    @Mock
    private Clock systemTime;
    @Mock
    private RollingMetrics<PartitionedCount<TimeoutableResult>> baseMetrics;
    @Mock
    private PartitionedCount<TimeoutableResult> counter;


    private RollingCounts<TimeoutableResult> counts;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(baseMetrics.current()).thenReturn(counter);
        when(counter.getMetricClazz()).thenReturn(TimeoutableResult.class);
        counts = new RollingCounts<TimeoutableResult>(baseMetrics);
    }

    @Test
    public void clazzComesFromCounter() {
        assertSame(TimeoutableResult.class, counts.getMetricClazz());
    }

    @Test
    public void writeUsesBaseMetrics() {
        when(baseMetrics.current(100L)).thenReturn(counter);

        counts.write(TimeoutableResult.ERROR, 3, 100L);

        verify(counter).add(TimeoutableResult.ERROR, 3);
    }

    @Test
    public void intervalsCallUsesNoOpForDefault() {
        IntervalIterator iterator = mock(IntervalIterator.class);
        when(baseMetrics.intervalsWithDefault(eq(100L), any(NoOpCounter.class))).thenReturn(iterator);

        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = counts.intervals(100L);

        assertSame(iterator, intervals);
    }
}
