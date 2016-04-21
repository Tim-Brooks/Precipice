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

package net.uncontended.precipice;

import net.uncontended.precipice.metrics.counts.LongAdderCounter;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.test_utils.TestCallables;
import net.uncontended.precipice.time.SystemTime;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.util.Simulation;
import net.uncontended.precipice.util.SimulationRejected;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallServiceTest {

    @Mock
    private GuardRail<TimeoutableResult, Rejected> guardRail;
    @Mock
    private CompletionContext<TimeoutableResult, Object> context;
    @Mock
    private PrecipiceFunction<TimeoutableResult, ExecutionContext> releaseFunction;

    private CallService<Rejected> service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new CallService<>(guardRail);

        when(guardRail.getClock()).thenReturn(new SystemTime());
        when(guardRail.releaseFunction()).thenReturn(releaseFunction);
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(guardRail.acquirePermits(eq(1L), anyLong())).thenThrow(new RejectedException(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.call(TestCallables.success(1));
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(guardRail.acquirePermits(eq(1L), anyLong())).thenThrow(new RejectedException(Rejected.CIRCUIT_OPEN));
            service.call(TestCallables.success(1));
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
        }
    }

    @Test
    public void callableIsExecuted() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);
        String expectedResult = "Success";

        String result = service.call(TestCallables.success(1));

        verify(releaseFunction).apply(eq(TimeoutableResult.SUCCESS), any(ExecutionContext.class));
        assertEquals(expectedResult, result);
    }

    @Test
    public void callableExceptionIsHandledAppropriately() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        RuntimeException exception = new RuntimeException();

        try {
            service.call(TestCallables.erred(exception));
        } catch (Exception e) {
            assertEquals(e, exception);
        }

        verify(releaseFunction).apply(eq(TimeoutableResult.ERROR), any(ExecutionContext.class));
    }

    @Test
    public void callableTimeoutExceptionIsHandledAppropriately() throws Exception {
        when(guardRail.acquirePermits(eq(1L), anyLong())).thenReturn(null);

        TimeoutException exception = new PrecipiceTimeoutException();

        try {
            service.call(TestCallables.erred(exception));
        } catch (Exception e) {
            assertEquals(e, exception);
        }

        verify(releaseFunction).apply(eq(TimeoutableResult.TIMEOUT), any(ExecutionContext.class));
    }

    @Test
    public void simulationTest() {
        final Random random = ThreadLocalRandom.current();
        GuardRailBuilder<TimeoutableResult, SimulationRejected> builder = new GuardRailBuilder<>();
        builder.name("Simulation")
                .resultMetrics(new LongAdderCounter<>(TimeoutableResult.class))
                .rejectedMetrics(new LongAdderCounter<>(SimulationRejected.class));

        GuardRail<TimeoutableResult, SimulationRejected> guardRail = builder.build();
        final CallService<SimulationRejected> callService = new CallService<>(guardRail);

        Map<TimeoutableResult, Callable<Long>> resultToCallable = new HashMap<>();
        resultToCallable.put(TimeoutableResult.SUCCESS, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long number = random.nextInt(10) + 1;
                try {
                    callService.call(TestCallables.success(0L), number);
                } catch (RejectedException e) {
                }
                return number;
            }
        });

        resultToCallable.put(TimeoutableResult.ERROR, new Callable<Long>() {
            @Override
            public Long call() {
                long number = random.nextInt(10) + 1;
                try {
                    callService.call(TestCallables.erred(new IOException()), number);
                } catch (Exception e) {
                }
                return number;
            }
        });

        resultToCallable.put(TimeoutableResult.TIMEOUT, new Callable<Long>() {
            @Override
            public Long call() {
                long number = random.nextInt(10) + 1;
                try {
                    callService.call(TestCallables.erred(new PrecipiceTimeoutException()), number);
                } catch (Exception e) {
                }
                return number;
            }
        });

        Simulation<TimeoutableResult> simulation = new Simulation<>(guardRail);
        simulation.run(resultToCallable);
    }
}
