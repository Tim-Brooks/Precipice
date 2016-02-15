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

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.RejectedException;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadPoolServiceTest {

    @Mock
    private GuardRail<Status, Rejected> guardRail;

    private ThreadPoolService<Rejected> service;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        executorService = PrecipiceExecutors.threadPoolExecutor("Test", 1, 100);
        service = new ThreadPoolService<>(executorService, guardRail);

        when(guardRail.getClock()).thenReturn(new SystemTime());
    }

    @After
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void controllerAndThreadPoolShutdownWhenShutdownCallMade() {
        service.shutdown();

        verify(guardRail).shutdown();

        assertTrue(executorService.isShutdown());
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(guardRail.acquirePermits(eq(1L), anyLong())).thenThrow(new RejectedException(Rejected
                    .MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.submit(TestCallable.success(), Long.MAX_VALUE);
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(guardRail.acquirePermits(eq(1L), anyLong())).thenThrow(new RejectedException(Rejected.CIRCUIT_OPEN));
            service.submit(TestCallable.success(), Long.MAX_VALUE);
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
        }
    }

    @Test
    public void callableIsSubmittedAndRan() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        PrecipiceFuture<Status, String> f = service.submit(TestCallable.success(), TimeoutService.MAX_TIMEOUT_MILLIS);

        assertEquals("Success", f.get());
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        PrecipicePromise<Status, String> promise = new Eventual<>(1L);

        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        service.complete(TestCallable.success("Same Promise"), promise, TimeoutService.NO_TIMEOUT);

        assertEquals("Same Promise", promise.future().get());
    }

    @Test
    public void promiseCanBeCompletedExternallyWithoutImpactingService() throws Exception {
        PrecipicePromise<Status, String> promise = mock(PrecipicePromise.class);
        PrecipiceFuture<Status, String> future = mock(PrecipiceFuture.class);

        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);
        when(promise.future()).thenReturn(future);
        when(future.isDone()).thenReturn(true);

        service.complete(TestCallable.success("Success"), promise, Long.MAX_VALUE);

        verify(promise).complete(Status.SUCCESS, "Success");
    }

    @Test
    public void submittedCallableWillTimeout() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<Status, String> future = service.submit(TestCallable.blocked(latch), 1);

        try {
            future.get();
            fail("Should have thrown ExecutionException from PrecipiceTimeoutException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PrecipiceTimeoutException);
        }

        assertEquals(Status.TIMEOUT, future.getStatus());
        assertTrue(future.getError() instanceof PrecipiceTimeoutException);

        latch.countDown();
    }

    @Test
    public void erredCallableWillReturnException() {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        RuntimeException exception = new RuntimeException();
        PrecipiceFuture<Status, String> future = service.submit(TestCallable.erred(exception), 100);

        try {
            future.get();
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            assertEquals(exception, e.getCause());
        }
        assertEquals(exception, future.getError());
        assertNull(future.getResult());
        assertEquals(Status.ERROR, future.getStatus());
    }

    @Test
    public void guardRailsClockIsUsedForStartTime() {
        long startNanos = 100L;
        Clock clock = mock(Clock.class);
        when(guardRail.getClock()).thenReturn(clock);
        when(clock.nanoTime()).thenReturn(startNanos);

        service.submit(TestCallable.success(), 100);

        verify(guardRail).acquirePermits(1L, startNanos);
    }
}
