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
import net.uncontended.precipice.GuardRailBuilder;
import net.uncontended.precipice.circuit.NoOpCircuitBreaker;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.MetricCounter;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;
import net.uncontended.precipice.util.Simulation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ThreadPoolServiceTest {

    @Mock
    private GuardRail<TimeoutableResult, Rejected> guardRail;

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

        PrecipiceFuture<TimeoutableResult, String> f = service.submit(TestCallable.success(), TimeoutService.MAX_TIMEOUT_MILLIS);

        assertEquals("Success", f.get());
        assertEquals(TimeoutableResult.SUCCESS, f.getStatus());
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        PrecipicePromise<TimeoutableResult, String> promise = new Eventual<>(1L);

        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        service.complete(TestCallable.success("Same Promise"), promise, TimeoutService.NO_TIMEOUT);

        assertEquals("Same Promise", promise.future().get());
    }

    @Test
    public void submittedCallableWillTimeout() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<TimeoutableResult, String> future = service.submit(TestCallable.blocked(latch), 1);

        try {
            future.get();
            fail("Should have thrown ExecutionException from PrecipiceTimeoutException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PrecipiceTimeoutException);
        }

        assertEquals(TimeoutableResult.TIMEOUT, future.getStatus());
        assertTrue(future.getError() instanceof PrecipiceTimeoutException);

        latch.countDown();
    }

    @Test
    public void erredCallableWillReturnException() {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        RuntimeException exception = new RuntimeException();
        PrecipiceFuture<TimeoutableResult, String> future = service.submit(TestCallable.erred(exception), 100);

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
        assertEquals(TimeoutableResult.ERROR, future.getStatus());
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

    @Test
    public void simulationTest() {
        GuardRailBuilder<TimeoutableResult, Rejected> builder = new GuardRailBuilder<>();
        final NoOpCircuitBreaker<Rejected> breaker = new NoOpCircuitBreaker<>(Rejected.CIRCUIT_OPEN);
        builder.name("Simulation")
                .resultMetrics(new MetricCounter<>(TimeoutableResult.class))
                .rejectedMetrics(new MetricCounter<>(Rejected.class))
                .addBackPressure(breaker);

        GuardRail<TimeoutableResult, Rejected> guardRail = builder.build();
        final ThreadPoolService<Rejected> callService = new ThreadPoolService<>(5, 10, guardRail);

        Map<TimeoutableResult, Runnable> resultToRunnable = new HashMap<>();
        resultToRunnable.put(TimeoutableResult.SUCCESS, new Runnable() {
            @Override
            public void run() {
                try {
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.success());
                    f.await();
                } catch (Exception e) {
                }
            }
        });

        resultToRunnable.put(TimeoutableResult.ERROR, new Runnable() {
            @Override
            public void run() {
                try {
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.erred(new IOException()));
                    f.await();
                } catch (Exception e) {
                }
            }
        });

        resultToRunnable.put(TimeoutableResult.TIMEOUT, new Runnable() {
            @Override
            public void run() {
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.blocked(latch), 1L);
                    f.await();
                    latch.countDown();
                } catch (Exception e) {
                }
            }
        });

        Map<Rejected, Runnable> rejectedToRunnable = new HashMap<>();
        rejectedToRunnable.put(Rejected.CIRCUIT_OPEN, new Runnable() {
            @Override
            public void run() {
                breaker.forceOpen();
                try {
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.success());
                    f.await();
                } catch (Exception e) {
                }
                breaker.forceClosed();
            }
        });

        Simulation<TimeoutableResult, Rejected> simulation = new Simulation<>(guardRail);
        simulation.run(resultToRunnable, rejectedToRunnable);
    }
}
