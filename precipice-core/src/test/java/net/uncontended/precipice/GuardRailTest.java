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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.metrics.counts.WritableCounts;
import net.uncontended.precipice.metrics.histogram.WritableLatency;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.test_utils.TestResult;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class GuardRailTest {

    @Mock
    private WritableCounts<TestResult> resultMetrics;
    @Mock
    private WritableCounts<Rejected> rejectedMetrics;
    @Mock
    private WritableLatency<TestResult> latencyMetrics;
    @Mock
    private BackPressure<Rejected> backPressure;
    @Mock
    private BackPressure<Rejected> backPressure2;
    @Mock
    private Clock clock;

    private GuardRail<TestResult, Rejected> guardRail;
    private GuardRailBuilder<TestResult, Rejected> builder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        builder = new GuardRailBuilder<>();
        builder.name("OldGuardRail Name");
        builder.resultMetrics(resultMetrics);
        builder.rejectedMetrics(rejectedMetrics);
        builder.resultLatency(latencyMetrics);
        builder.addBackPressure(backPressure);
        builder.addBackPressure(backPressure2);
        builder.clock(clock);
    }

    @Test
    public void backPressureMechanismsAreSetupWithGuardRail() {
        this.guardRail = builder.build();

        verify(backPressure).registerGuardRail(guardRail);
        verify(backPressure2).registerGuardRail(guardRail);
    }

    @Test
    public void acquirePermitReturnsNullIfNoRejections() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(1L, 10L)).thenReturn(null);
        when(backPressure2.acquirePermit(1L, 10L)).thenReturn(null);

        assertNull(guardRail.acquirePermits(1L, 10L));
    }

    @Test
    public void acquirePermitReturnsReasonIfRejected() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(1L, 10L)).thenReturn(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, guardRail.acquirePermits(1L, 10L));

        verify(backPressure2).registerGuardRail(guardRail);
        verifyNoMoreInteractions(backPressure2);
    }

    @Test
    public void priorPermitsAreReleasedIfARejectionEventuallyOccurs() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(2L, 22L)).thenReturn(null);
        when(backPressure2.acquirePermit(2L, 22L)).thenReturn(Rejected.CIRCUIT_OPEN);

        assertSame(Rejected.CIRCUIT_OPEN, guardRail.acquirePermits(2L, 22L));

        InOrder inOrder = inOrder(backPressure);
        inOrder.verify(backPressure).acquirePermit(2L, 22L);
        inOrder.verify(backPressure).releasePermit(2L, 22L);
    }

    // TODO: Add tests for rejected metrics

    @Test
    public void releaseCausesBackPressureReleasesToBeCalled() {
        guardRail = builder.build();

        guardRail.releasePermitsWithoutResult(2L, 100L);

        InOrder inOrder = inOrder(backPressure, backPressure2);
        inOrder.verify(backPressure).releasePermit(2L, 100L);
        inOrder.verify(backPressure2).releasePermit(2L, 100L);
    }

    @Test
    public void releaseWithResultIncrementsMetricsAndCausesBackPressureReleasesToBeCalled() {
        guardRail = builder.build();
        TestResult result = TestResult.SUCCESS;

        guardRail.releasePermits(2L, result, 10L, 100L);

        verify(resultMetrics).write(result, 2L, 100L);
        verify(latencyMetrics).write(result, 2L, 90L, 100L);

        InOrder inOrder = inOrder(backPressure, backPressure2);
        inOrder.verify(backPressure).releasePermit(2L, result, 100L);
        inOrder.verify(backPressure2).releasePermit(2L, result, 100L);
    }

    @Test
    public void releaseWithContextIncrementsMetricsAndCausesBackPressureReleasesToBeCalled() {
        guardRail = builder.build();
        TestResult result = TestResult.SUCCESS;

        Eventual<TestResult, String> context = new Eventual<>(2L, 10L);

        guardRail.releasePermits(context, result, 100L);

        verify(resultMetrics).write(result, 2L, 100L);
        verify(latencyMetrics).write(result, 2L, 90L, 100L);

        InOrder inOrder = inOrder(backPressure, backPressure2);
        inOrder.verify(backPressure).releasePermit(2L, result, 100L);
        inOrder.verify(backPressure2).releasePermit(2L, result, 100L);
    }

    @Test
    public void releaseFunctionReleasesPermitsAndIncrementsMetrics() {
        guardRail = builder.build();
        when(clock.nanoTime()).thenReturn(110L);

        Eventual<TestResult, String> context = new Eventual<>(2L, 10L);

        PrecipiceFunction<TestResult, ExecutionContext> fn = guardRail.releaseFunction();

        TestResult result = TestResult.ERROR;
        fn.apply(result, context);

        verify(resultMetrics).write(result, 2L, 110L);
        verify(latencyMetrics).write(result, 2L, 100L, 110L);

        InOrder inOrder = inOrder(backPressure, backPressure2);
        inOrder.verify(backPressure).releasePermit(2L, result, 110L);
        inOrder.verify(backPressure2).releasePermit(2L, result, 110L);
    }
}
