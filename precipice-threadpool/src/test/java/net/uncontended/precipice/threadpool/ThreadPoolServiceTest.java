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
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.counts.TotalCounter;
import net.uncontended.precipice.metrics.histogram.NoOpLatency;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;
import net.uncontended.precipice.timeout.DelayQueueTimeoutService;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.util.Simulation;
import net.uncontended.precipice.util.SimulationRejected;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
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
    public void threadPoolShutdownWhenShutdownCallMade() {
        service.shutdown();

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

        PrecipiceFuture<TimeoutableResult, String> f = service.submit(TestCallable.success(), DelayQueueTimeoutService.MAX_TIMEOUT_MILLIS);

        assertEquals("Success", f.get());
        assertEquals(TimeoutableResult.SUCCESS, f.getResult());
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        PrecipicePromise<TimeoutableResult, String> promise = new Eventual<>(1L);

        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        service.complete(TestCallable.success("Same Promise"), promise);

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

        assertEquals(TimeoutableResult.TIMEOUT, future.getResult());
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
        assertNull(future.getValue());
        assertEquals(TimeoutableResult.ERROR, future.getResult());
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
        GuardRailBuilder<TimeoutableResult, SimulationRejected> builder =
                new GuardRailBuilder<TimeoutableResult, SimulationRejected>()
                        .name("Simulation")
                        .resultMetrics(new TotalCounter<TimeoutableResult>(TimeoutableResult.class))
                        .rejectedMetrics(new TotalCounter<>(SimulationRejected.class))
                        .resultLatency(new NoOpLatency<>(TimeoutableResult.class));

        GuardRail<TimeoutableResult, SimulationRejected> guardRail = builder.build();
        final ThreadPoolService<SimulationRejected> callService = new ThreadPoolService<>(5, 10, guardRail);

        Map<TimeoutableResult, Callable<Long>> resultToCallable = new HashMap<>();
        resultToCallable.put(TimeoutableResult.SUCCESS, new Callable<Long>() {
            @Override
            public Long call() throws InterruptedException {
                try {
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.success());
                    f.await();
                } catch (RejectedException e) {
                }
                return 1L;
            }
        });

        resultToCallable.put(TimeoutableResult.ERROR, new Callable<Long>() {
            @Override
            public Long call() throws InterruptedException {
                try {
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.erred(new IOException()));
                    f.await();
                } catch (RejectedException e) {
                }
                return 1L;
            }
        });

        resultToCallable.put(TimeoutableResult.TIMEOUT, new Callable<Long>() {
            @Override
            public Long call() throws InterruptedException {
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    PrecipiceFuture<TimeoutableResult, String> f = callService.submit(TestCallable.blocked(latch), 1L);
                    f.await();
                    latch.countDown();
                } catch (RejectedException e) {
                }
                return 1L;
            }
        });

        Simulation<TimeoutableResult> simulation = new Simulation<>(guardRail);
        simulation.run(resultToCallable);
    }
}
