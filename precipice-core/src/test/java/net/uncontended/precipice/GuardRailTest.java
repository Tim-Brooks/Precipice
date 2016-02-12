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

import net.uncontended.precipice.backpressure.BPRejectedException;
import net.uncontended.precipice.metrics.TotalCountMetrics;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GuardRailTest {

    @Mock
    private TotalCountMetrics<Status> metrics;
    @Mock
    private TotalCountMetrics<Rejected> rejectedMetrics;
    @Mock
    private LatencyMetrics<Status> latencyMetrics;
    @Mock
    private BackPressure<Rejected> backPressure;
    @Mock
    private BackPressure<Rejected> backPressure2;
    @Mock
    private Clock clock;

    private OldGuardRail<Status, Rejected> guardRail;
    private GuardRailBuilder<Status, Rejected> builder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        builder = new GuardRailBuilder<>();
        builder.name("OldGuardRail Name");
        builder.resultMetrics(metrics);
        builder.rejectedMetrics(rejectedMetrics);
        builder.resultLatency(latencyMetrics);
        builder.addBackPressure(backPressure);
        builder.addBackPressure(backPressure2);
        builder.clock(clock);
    }

    @Test
    public void exceptionIfShutdown() {
        guardRail = builder.build();

        guardRail.shutdown();
        try {
            guardRail.acquirePermitOrGetRejectedReason(1L);
            fail("Exception should have been thrown due to controllable being shutdown.");
        } catch (IllegalStateException e) {
            assertEquals("Service has been shutdown.", e.getMessage());
        }

    }

    @Test
    public void acquirePermitReturnsNullIfNoRejections() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(1L, 10L)).thenReturn(null);
        when(backPressure2.acquirePermit(1L, 10L)).thenReturn(null);

        assertNull(guardRail.acquirePermitOrGetRejectedReason(1L, 10L));
    }

    @Test
    public void acquirePermitReturnsReasonIfRejected() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(1L, 10L)).thenReturn(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, guardRail.acquirePermitOrGetRejectedReason(1L, 10L));

        verifyZeroInteractions(backPressure2);
    }

    @Test
    public void priorPermitsAreReleasedIfARejectionEventuallyOccurs() {
        guardRail = builder.build();

        when(backPressure.acquirePermit(2L, 22L)).thenReturn(null);
        when(backPressure2.acquirePermit(2L, 22L)).thenReturn(Rejected.CIRCUIT_OPEN);

        assertSame(Rejected.CIRCUIT_OPEN, guardRail.acquirePermitOrGetRejectedReason(2L, 22L));

        InOrder inOrder = inOrder(backPressure);
        inOrder.verify(backPressure).acquirePermit(2L, 22L);
        inOrder.verify(backPressure).releasePermit(2L, 22L);
    }

    @Test
    public void getPromiseReturnsPromiseWithMetricsCallback() {
        guardRail = builder.build();

        long startTime = 10L;

        PrecipicePromise<Status, String> promise = guardRail.getPromise(1L, startTime);

        long endTime = 100L;
        when(clock.nanoTime()).thenReturn(endTime);

        promise.complete(Status.SUCCESS, "hello");

        verify(backPressure).releasePermit(1, Status.SUCCESS, endTime);
        verify(backPressure2).releasePermit(1, Status.SUCCESS, endTime);
        verify(metrics).incrementMetricCount(Status.SUCCESS, endTime);
        verify(latencyMetrics).recordLatency(Status.SUCCESS, 90L, endTime);
    }

    @Test
    public void acquirePermitAndGetPromiseThrowsIfRejected() {
        guardRail = builder.build();

        long startTime = 10L;
        when(clock.nanoTime()).thenReturn(startTime);
        when(backPressure.acquirePermit(2L, 10L)).thenReturn(Rejected.CIRCUIT_OPEN);

        try {
            guardRail.acquirePermitAndGetPromise(2L);
        } catch (BPRejectedException e) {
            assertSame(Rejected.CIRCUIT_OPEN, e.reason);
        }

        verify(backPressure).acquirePermit(2L, 10L);
        verifyNoMoreInteractions(backPressure);
        verify(rejectedMetrics).incrementMetricCount(Rejected.CIRCUIT_OPEN, startTime);
        verifyZeroInteractions(latencyMetrics);
        verifyZeroInteractions(backPressure2);
    }

    // TODO: Add tests for more scenarios

}
