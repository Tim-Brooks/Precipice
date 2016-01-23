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

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.concurrent.LongSemaphore;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ControllerTest {

    @Mock
    private ActionMetrics<Status> metrics;
    @Mock
    private LatencyMetrics<Status> latencyMetrics;
    @Mock
    private PrecipiceSemaphore semaphore;
    @Mock
    private CircuitBreaker breaker;
    @Mock
    private Clock clock;

    private Controller<Status> controller;
    private ControllerProperties<Status> properties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        properties = new ControllerProperties<>(Status.class);
        properties.circuitBreaker(breaker);
        properties.actionMetrics(metrics);
        properties.latencyMetrics(latencyMetrics);
        properties.semaphore(semaphore);
        properties.clock(clock);
    }

    @Test
    public void capacityAndPendingCallsDelegateToSemaphore() {
        properties.semaphore(new LongSemaphore(10));
        controller = new Controller<>("Controller Name", properties);

        assertEquals(10, controller.remainingCapacity());
        assertEquals(0, controller.pendingCount());

        when(breaker.allowAction()).thenReturn(true);
        controller.acquirePermitOrGetRejectedReason();

        assertEquals(9, controller.remainingCapacity());
        assertEquals(1, controller.pendingCount());
    }

    @Test
    public void exceptionIfShutdown() {
        controller = new Controller<>("Controller Name", properties);

        controller.shutdown();
        try {
            controller.acquirePermitOrGetRejectedReason();
            fail("Exception should have been thrown due to controller being shutdown.");
        } catch (IllegalStateException e) {
            assertEquals("Service has been shutdown.", e.getMessage());
        }

    }

    @Test
    public void acquirePermitOrGetRejectedReasonReasonIfRejected() {
        controller = new Controller<>("Controller Name", properties);

        when(semaphore.acquirePermit(1)).thenReturn(true);
        when(breaker.allowAction()).thenReturn(true);

        assertNull(controller.acquirePermitOrGetRejectedReason());

        when(semaphore.acquirePermit(1)).thenReturn(false);
        when(breaker.allowAction()).thenReturn(true);

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, controller.acquirePermitOrGetRejectedReason());

        when(semaphore.acquirePermit(1)).thenReturn(true);
        when(breaker.allowAction()).thenReturn(false);

        assertSame(Rejected.CIRCUIT_OPEN, controller.acquirePermitOrGetRejectedReason());
    }

    @Test
    public void getPromiseReturnsPromiseWithMetricsCallback() {
        controller = new Controller<>("Controller Name", properties);

        long startTime = 10L;

        PrecipicePromise<Status, String> promise = controller.getPromise(startTime, null);

        when(clock.nanoTime()).thenReturn(100L);

        promise.complete(Status.SUCCESS, "hello");

        verify(breaker).informBreakerOfResult(true, 100L);
        verify(metrics).incrementMetricCount(Status.SUCCESS, 100L);
        verify(latencyMetrics).recordLatency(Status.SUCCESS, 90L, 100L);
        verify(semaphore).releasePermit(1);
    }

    @Test
    public void acquirePermitAndGetPromiseThrowsIfMaxConcurrencyRejected() {
        controller = new Controller<>("Controller Name", properties);
        verify(breaker).setActionMetrics(metrics);

        long startTime = 10L;
        when(clock.nanoTime()).thenReturn(startTime);
        when(semaphore.acquirePermit(1L)).thenReturn(false);

        try {
            controller.acquirePermitAndGetPromise();
        } catch (RejectedActionException e) {
            assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        verify(semaphore).acquirePermit(1L);
        verifyNoMoreInteractions(semaphore);
        verify(metrics).incrementRejectionCount(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, startTime);
        verifyZeroInteractions(latencyMetrics);
        verifyZeroInteractions(breaker);
    }

    @Test
    public void acquirePermitAndGetPromiseThrowsIfCircuitOpen() {
        controller = new Controller<>("Controller Name", properties);
        verify(breaker).setActionMetrics(metrics);

        long startTime = 10L;

        when(clock.nanoTime()).thenReturn(startTime);
        when(semaphore.acquirePermit(1L)).thenReturn(true);
        when(breaker.allowAction()).thenReturn(false);

        try {
            controller.acquirePermitAndGetPromise();
        } catch (RejectedActionException e) {
            assertSame(Rejected.CIRCUIT_OPEN, e.reason);
        }

        InOrder inOrder = inOrder(semaphore);
        inOrder.verify(semaphore).acquirePermit(1L);
        inOrder.verify(semaphore).releasePermit(1L);
        verify(metrics).incrementRejectionCount(Rejected.CIRCUIT_OPEN, startTime);
        verifyZeroInteractions(latencyMetrics);
    }

}
