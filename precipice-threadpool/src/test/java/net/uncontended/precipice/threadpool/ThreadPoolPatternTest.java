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
import net.uncontended.precipice.Status;
import net.uncontended.precipice.backpressure.PromiseFactory;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.metrics.TotalCountMetrics;
import net.uncontended.precipice.pattern.Pattern;
import net.uncontended.precipice.pattern.PatternAction;
import net.uncontended.precipice.pattern.SingleReaderSequence;
import net.uncontended.precipice.semaphore.PrecipiceSemaphore;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.timeout.TimeoutService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ThreadPoolPatternTest {

//    private Object context1 = new Object();
//    private Object context2 = new Object();
//    private Object context3 = new Object();
//    @Mock
//    private ThreadPoolService service1;
//    @Mock
//    private ThreadPoolService service2;
//    @Mock
//    private ThreadPoolService service3;
//    @Mock
//    private GuardRail<Status, ?> guardRail1;
//    @Mock
//    private GuardRail<Status, ?> guardRail2;
//    @Mock
//    private GuardRail<Status, ?> guardRail3;
//    @Mock
//    private PromiseFactory<?> promiseFactory1;
//    @Mock
//    private PromiseFactory<?> promiseFactory2;
//    @Mock
//    private PromiseFactory<?> promiseFactory3;
//    @Mock
//    private ExecutorService executor1;
//    @Mock
//    private ExecutorService executor2;
//    @Mock
//    private ExecutorService executor3;
//    @Mock
//    private TimeoutService timeoutService1;
//    @Mock
//    private TimeoutService timeoutService2;
//    @Mock
//    private TimeoutService timeoutService3;
//    @Mock
//    private GuardRail<Status, Rejected> guardRail;
//    @Mock
//    private PromiseFactory<Rejected> promiseFactory;
//    @Mock
//    private Clock clock;
//    @Mock
//    private PrecipiceSemaphore semaphore;
//    @Mock
//    private TotalCountMetrics<Rejected> metrics;
//    @Mock
//    private Pattern<Status, ThreadPoolService<?>> pattern;
//    @Mock
//    private PatternAction<String, Object> action;
//    @Captor
//    private ArgumentCaptor<ThreadPoolTask<Status>> task1Captor;
//    @Captor
//    private ArgumentCaptor<ThreadPoolTask<Status>> task2Captor;
//
//    private ThreadPoolPattern<Object> poolPattern;
//    private long submitTimeNanos = 10L;
//
//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//
//        Map<ThreadPoolService<?>, Object> services = new LinkedHashMap<>();
//        services.put(service1, context1);
//        services.put(service2, context2);
//        services.put(service3, context3);
//        this.poolPattern = new ThreadPoolPattern<>(services, guardRail, pattern, promiseFactory);
//
//        when(service1.guardRail()).thenReturn(guardRail1);
//        when(service2.guardRail()).thenReturn(guardRail2);
//        when(service3.guardRail()).thenReturn(guardRail3);
//        when(service1.getPromiseFactory()).thenReturn(promiseFactory1);
//        when(service2.getPromiseFactory()).thenReturn(promiseFactory2);
//        when(service3.getPromiseFactory()).thenReturn(promiseFactory3);
//        when(service1.getExecutor()).thenReturn(executor1);
//        when(service2.getExecutor()).thenReturn(executor2);
//        when(service3.getExecutor()).thenReturn(executor3);
//        when(service1.getTimeoutService()).thenReturn(timeoutService1);
//        when(service2.getTimeoutService()).thenReturn(timeoutService2);
//        when(service3.getTimeoutService()).thenReturn(timeoutService3);
//
//        when(guardRail.getClock()).thenReturn(clock);
//        when(guardRail.getRejectedMetrics()).thenReturn(metrics);
//        when(clock.nanoTime()).thenReturn(submitTimeNanos);
//
//        when(action.call(context1)).thenReturn("Service1");
//        when(action.call(context2)).thenReturn("Service2");
//        when(action.call(context3)).thenReturn("Service3");
//    }
//
//    @Test
//    public void actionsSubmittedToServices() throws Exception {
//        SingleReaderSequence<ThreadPoolService<?>> iterable = prepIterable(service1, service3);
//        Eventual<Status, Object> parent = new Eventual<>(1L, submitTimeNanos);
//        Eventual<Status, Object> child1 = new Eventual<>(1L, submitTimeNanos, parent);
//        Eventual<Status, Object> child2 = new Eventual<>(1L, submitTimeNanos, parent);
//        long millisTimeout = 100L;
//
//        when(guardRail.acquirePermits(1L)).thenReturn(null);
//        when(pattern.getPrecipices(1L, submitTimeNanos)).thenReturn(iterable);
//        when(promiseFactory.getPromise(1L, submitTimeNanos)).thenReturn(parent);
//        when(promiseFactory1.getPromise(guardRail, 1L, submitTimeNanos, parent)).thenReturn(child1);
//        when(promiseFactory3.getPromise(guardRail, 1L, submitTimeNanos, parent)).thenReturn(child2);
//
//        PrecipiceFuture<Status, String> f = poolPattern.submit(action, millisTimeout);
//
//        verifyZeroInteractions(service2);
//        verify(promiseFactory1).getPromise(guardRail, 1L, submitTimeNanos, parent);
//        verify(promiseFactory3).getPromise(guardRail, 1L, submitTimeNanos, parent);
//        verify(executor1).execute(task1Captor.capture());
//        verify(executor3).execute(task2Captor.capture());
//        verify(timeoutService1).scheduleTimeout(task1Captor.capture());
//        verify(timeoutService3).scheduleTimeout(task2Captor.capture());
//
//        ThreadPoolTask<Status> task1 = task1Captor.getAllValues().get(0);
//        ThreadPoolTask<Status> task12 = task1Captor.getAllValues().get(1);
//        ThreadPoolTask<Status> task2 = task2Captor.getAllValues().get(0);
//        ThreadPoolTask<Status> task22 = task2Captor.getAllValues().get(1);
//
//        assertSame(task1, task12);
//        assertSame(task2, task22);
//        assertEquals(millisTimeout, task1.getMillisRelativeTimeout());
//        assertEquals(millisTimeout, task2.getMillisRelativeTimeout());
//
//        long expectedNanoTimeout = submitTimeNanos + TimeUnit.MILLISECONDS.toNanos(millisTimeout);
//        assertEquals(expectedNanoTimeout, task1.nanosAbsoluteTimeout);
//        assertEquals(expectedNanoTimeout, task2.nanosAbsoluteTimeout);
//
//        assertNull(f.getStatus());
//        task1.run();
//        task2.run();
//        assertEquals(Status.SUCCESS, f.getStatus());
//        assertEquals("Service1", f.result());
//
//        PrecipiceFuture<Status, Object> future1 = child1.future();
//        PrecipiceFuture<Status, Object> future2 = child2.future();
//        assertEquals("Service1", future1.result());
//        assertEquals(Status.SUCCESS, future1.getStatus());
//        assertEquals("Service3", future2.result());
//        assertEquals(Status.SUCCESS, future2.getStatus());
//    }
//
//    @Test
//    public void ifNoServiceReturnedThenAllRejected() throws Exception {
//        SingleReaderSequence<ThreadPoolService> iterable = prepIterable();
//        long millisTimeout = 100L;
//
//        when(guardRail.acquirePermitOrGetRejectedReason()).thenReturn(null);
//        when(pattern.getPrecipices(1L, submitTimeNanos)).thenReturn(iterable);
//
//        try {
//            poolPattern.submit(action, millisTimeout);
//            fail("Should have been rejected");
//        } catch (RejectedException e) {
//            assertEquals(Rejected.ALL_SERVICES_REJECTED, e.reason);
//        }
//
//        verify(semaphore).releasePermit(1);
//        verify(metrics).incrementRejectionCount(Rejected.ALL_SERVICES_REJECTED, submitTimeNanos);
//
//        verifyZeroInteractions(service1);
//        verifyZeroInteractions(service2);
//        verifyZeroInteractions(service3);
//    }
//
//    @Test
//    public void rejectedActionsHandled() throws Exception {
//        when(guardRail.acquirePermitOrGetRejectedReason()).thenReturn(Rejected.CIRCUIT_OPEN);
//
//        try {
//            poolPattern.submit(action, 100L);
//            fail("Should have been rejected");
//        } catch (RejectedException e) {
//            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
//        }
//        verify(metrics).incrementRejectionCount(Rejected.CIRCUIT_OPEN, submitTimeNanos);
//
//        verifyZeroInteractions(service1);
//        verifyZeroInteractions(service2);
//        verifyZeroInteractions(service3);
//
//        when(guardRail.acquirePermitOrGetRejectedReason()).thenReturn(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
//
//        try {
//            poolPattern.submit(action, 100L);
//            fail("Should have been rejected");
//        } catch (RejectedException e) {
//            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
//        }
//        verify(metrics).incrementRejectionCount(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, submitTimeNanos);
//
//        verifyZeroInteractions(service1);
//        verifyZeroInteractions(service2);
//        verifyZeroInteractions(service3);
//    }
//
    private SingleReaderSequence<ThreadPoolService<?>> prepIterable(ThreadPoolService... services) {
        SingleReaderSequence<ThreadPoolService<?>> iterable = new SingleReaderSequence<>(services.length);

        for (ThreadPoolService<?> service : services) {
            iterable.add(service);
        }
        return iterable;
    }
}
