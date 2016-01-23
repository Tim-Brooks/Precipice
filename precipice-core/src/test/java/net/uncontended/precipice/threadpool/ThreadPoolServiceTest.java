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

import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.RejectedActionException;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.test_utils.TestCallables;
import net.uncontended.precipice.utils.PrecipiceExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadPoolServiceTest {

    @Mock
    private Controller<Status> controller;

    private ThreadPoolService service;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        executorService = PrecipiceExecutors.threadPoolExecutor("Test", 1, 100);
        service = new ThreadPoolService(executorService, controller);
    }

    @After
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void controllerAndThreadPoolShutdownWhenShutdownCallMade() {
        service.shutdown();

        verify(controller).shutdown();

        assertTrue(executorService.isShutdown());
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(controller.acquirePermitAndGetPromise()).thenThrow(new RejectedActionException(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.submit(TestCallables.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(controller.acquirePermitAndGetPromise()).thenThrow(new RejectedActionException(Rejected.CIRCUIT_OPEN));
            service.submit(TestCallables.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
    }

//    @Test
//    public void actionsReleaseSemaphorePermitWhenComplete() throws Exception {
//        ServiceProperties properties = new ServiceProperties();
//        properties.concurrencyLevel(1);
//        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
//        service = Services.submissionService("Test", 1, properties);
//        int iterations = new Random().nextInt(50);
//        for (int i = 0; i < iterations; ++i) {
//            int j = 0;
//            while (true) {
//                try {
//                    PrecipiceFuture<Status, String> future = service.submit(TestActions.successAction(1), 100L);
//                    future.get();
//                    break;
//                } catch (RejectedActionException e) {
//                    Thread.sleep(5);
//                    if (j == 20) {
//                        fail("Continue to receive action rejects.");
//                    }
//                }
//                ++j;
//            }
//        }
//    }
//
//    @Test
//    public void actionIsSubmittedAndRan() throws Exception {
//        PrecipiceFuture<Status, String> f = service.submit(TestActions.successAction(1), 500);
//
//        assertEquals("Success", f.get());
//        assertEquals(Status.SUCCESS, f.getStatus());
//    }
//
//    @Test
//    public void futureIsPendingUntilSubmittedActionFinished() throws Exception {
//        CountDownLatch latch = new CountDownLatch(1);
//        PrecipiceFuture<Status, String> f = service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
//        assertNull(f.getStatus());
//        latch.countDown();
//        f.get();
//        assertEquals(Status.SUCCESS, f.getStatus());
//    }
//
//    @Test
//    public void promisePassedToExecutorWillBeCompleted() throws Exception {
//        PrecipicePromise<Status, String> promise = new Eventual<>();
//        service.complete(TestActions.successAction(50, "Same Promise"), promise, Long.MAX_VALUE);
//
//        assertEquals("Same Promise", promise.future().get());
//    }
//
//    @Test
//    public void promiseWillNotBeCompletedTwice() throws Exception {
//        CountDownLatch latch = new CountDownLatch(1);
//        PrecipicePromise<Status, String> promise = new Eventual<>();
//
//        service.complete(TestActions.blockedAction(latch), promise, Long.MAX_VALUE);
//
//        promise.complete(Status.SUCCESS, "CompleteOnThisThread");
//        latch.countDown();
//
//        for (int i = 0; i < 10; ++i) {
//            Thread.sleep(5);
//            assertEquals("CompleteOnThisThread", promise.future().get());
//        }
//    }
//
//    @Test
//    public void submittedActionWillTimeout() throws Exception {
//        PrecipiceFuture<Status, String> future = service.submit(TestActions.blockedAction(new CountDownLatch
//                (1)), 1);
//
//        try {
//            future.get();
//            fail("Should have thrown ExecutionException from ActionTimeoutException");
//        } catch (ExecutionException e) {
//            assertTrue(e.getCause() instanceof ActionTimeoutException);
//        }
//
//        assertEquals(Status.TIMEOUT, future.getStatus());
//    }
//
//    @Test
//    public void erredActionWillReturnException() {
//        RuntimeException exception = new RuntimeException();
//        PrecipiceFuture<Status, String> future = service.submit(TestActions.erredAction(exception), 100);
//
//        try {
//            future.get();
//            fail();
//        } catch (InterruptedException e) {
//            fail();
//        } catch (ExecutionException e) {
//            assertEquals(exception, e.getCause());
//        }
//        assertEquals(exception, future.error());
//        assertNull(future.result());
//        assertEquals(Status.ERROR, future.getStatus());
//    }
//
//    @Test
//    public void resultMetricsUpdated() throws Exception {
//        CountDownLatch timeoutLatch = new CountDownLatch(1);
//        CountDownLatch blockingLatch = new CountDownLatch(3);
//
//        PrecipiceFuture<Status, String> errorF = service.submit(TestActions.erredAction(new IOException()), Long.MAX_VALUE);
//        PrecipiceFunction<Status, Throwable> callback = TestCallbacks.latchedCallback(blockingLatch);
//        errorF.onError(callback);
//
//        PrecipiceFuture<Status, String> timeOutF = service.submit(TestActions.blockedAction(timeoutLatch), 1);
//        PrecipiceFunction<Status, Throwable> callback2 = TestCallbacks.latchedCallback(blockingLatch);
//        errorF.onError(callback2);
//
//        PrecipiceFuture<Status, String> successF = service.submit(TestActions.successAction(50, "Success"), Long.MAX_VALUE);
//        PrecipiceFunction<Status, String> callback3 = TestCallbacks.latchedCallback(blockingLatch);
//        successF.onSuccess(callback3);
//
//        for (PrecipiceFuture<Status, String> f : Arrays.asList(errorF, timeOutF, successF)) {
//            try {
//                f.get();
//            } catch (ExecutionException e) {
//            }
//        }
//
//        ActionMetrics<Status> metrics = (ActionMetrics<Status>) service.getActionMetrics();
//        Map<Object, Integer> expectedCounts = new HashMap<>();
//        expectedCounts.put(Status.SUCCESS, 1);
//        expectedCounts.put(Status.ERROR, 1);
//        expectedCounts.put(Status.TIMEOUT, 1);
//
//        blockingLatch.await();
//
//        assertNewMetrics(metrics, expectedCounts);
//    }
//
//    @Test
//    public void rejectedMetricsUpdated() throws Exception {
//        ServiceProperties properties = new ServiceProperties();
//        properties.concurrencyLevel(1);
//        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
//        service = Services.submissionService("Test", 1, properties);
//        CountDownLatch latch = new CountDownLatch(1);
//        CountDownLatch blockingLatch = new CountDownLatch(1);
//        PrecipiceFunction<Status, String> callback = TestCallbacks.latchedCallback(blockingLatch);
//
//        PrecipiceFuture<Status, String> f = service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
//        f.onSuccess(callback);
//
//        try {
//            service.submit(TestActions.successAction(1), Long.MAX_VALUE);
//            f.onSuccess(callback);
//        } catch (RejectedActionException e) {
//            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
//        }
//
//        latch.countDown();
//        f.get();
//        blockingLatch.await();
//        service.getCircuitBreaker().forceOpen();
//
//        int maxConcurrencyErrors = 1;
//        for (int i = 0; i < 5; ++i) {
//            try {
//                service.submit(TestActions.successAction(1), Long.MAX_VALUE);
//            } catch (RejectedActionException e) {
//                if (e.reason == Rejected.CIRCUIT_OPEN) {
//                    break;
//                } else {
//                    maxConcurrencyErrors++;
//                }
//            }
//        }
//
//        Map<Object, Integer> expectedCounts = new HashMap<>();
//        expectedCounts.put(Status.SUCCESS, 1);
//        expectedCounts.put(Rejected.CIRCUIT_OPEN, 1);
//        expectedCounts.put(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, maxConcurrencyErrors);
//
//        assertNewMetrics((ActionMetrics<Status>) service.getActionMetrics(), expectedCounts);
//    }
//
//    @Test
//    public void attachedCallbacksWillBeExecutedOnCompletion() throws Exception {
//        final AtomicReference<Throwable> error = new AtomicReference<>();
//        final AtomicReference<String> result = new AtomicReference<>();
//        final AtomicBoolean isTimedOut = new AtomicBoolean(false);
//
//
//        CountDownLatch blockingLatch = new CountDownLatch(1);
//        final CountDownLatch callbackLatch = new CountDownLatch(3);
//
//        IOException exception = new IOException();
//        PrecipiceFuture<Status, String> errorF = service.submit(TestActions.erredAction(exception), 100);
//        errorF.onError(new PrecipiceFunction<Status, Throwable>() {
//            @Override
//            public void apply(Status status, Throwable argument) {
//                error.set(argument);
//                callbackLatch.countDown();
//            }
//        });
//
//        PrecipiceFuture<Status, String> timeOutF = service.submit(TestActions.blockedAction(blockingLatch), 1);
//        timeOutF.onError(new PrecipiceFunction<Status, Throwable>() {
//            @Override
//            public void apply(Status status, Throwable argument) {
//                isTimedOut.set(true);
//                callbackLatch.countDown();
//            }
//        });
//
//        String resultString = "Success";
//        final PrecipiceFuture<Status, String> successF = service.submit(TestActions.successAction(50, resultString), Long.MAX_VALUE);
//        successF.onSuccess(new PrecipiceFunction<Status, String>() {
//            @Override
//            public void apply(Status status, String argument) {
//                result.set(argument);
//                callbackLatch.countDown();
//            }
//        });
//
//        callbackLatch.await();
//        blockingLatch.countDown();
//
//        assertSame(exception, error.get());
//        assertSame(resultString, successF.get());
//        assertTrue(isTimedOut.get());
//    }
//
//    @Test
//    public void metricsUpdatedEvenIfPromiseAlreadyCompleted() throws Exception {
//        CountDownLatch timeoutLatch = new CountDownLatch(1);
//        PrecipicePromise<Status, String> errP = new Eventual<>();
//        PrecipicePromise<Status, String> timeoutP = new Eventual<>();
//        PrecipicePromise<Status, String> successP = new Eventual<>();
//        errP.complete(Status.SUCCESS, "Done");
//        timeoutP.complete(Status.SUCCESS, "Done");
//        successP.complete(Status.SUCCESS, "Done");
//
//        service.complete(TestActions.erredAction(new IOException()), errP, 100);
//        service.complete(TestActions.successAction(50, "Success"), successP, Long.MAX_VALUE);
//        service.complete(TestActions.blockedAction(timeoutLatch), timeoutP, 1);
//
//        ActionMetrics<Status> metrics = (ActionMetrics<Status>) service.getActionMetrics();
//        for (int i = 0; i <= 20; ++i) {
//            if (metrics.getMetricCountForTimePeriod(Status.SUCCESS, 5, TimeUnit.SECONDS) == 1 &&
//                    metrics.getMetricCountForTimePeriod(Status.ERROR, 5, TimeUnit.SECONDS) == 1 &&
//                    metrics.getMetricCountForTimePeriod(Status.TIMEOUT, 5, TimeUnit.SECONDS) == 1) {
//                break;
//            } else {
//                if (i == 20) {
//                    fail("Never encountered a timeout.");
//                } else {
//                    Thread.sleep(10);
//                }
//            }
//        }
//
//        timeoutLatch.countDown();
//
//        Map<Object, Integer> expectedCounts = new HashMap<>();
//        expectedCounts.put(Status.SUCCESS, 1);
//        expectedCounts.put(Status.ERROR, 1);
//        expectedCounts.put(Status.TIMEOUT, 1);
//
//        assertNewMetrics(metrics, expectedCounts);
//    }
//
//    @Test
//    public void semaphoreReleasedDespiteCallbackException() throws Exception {
//        ServiceProperties properties = new ServiceProperties();
//        properties.concurrencyLevel(1);
//        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
//        service = Services.submissionService("Test", 1, properties);
//        CountDownLatch latch = new CountDownLatch(1);
//
//        PrecipiceFuture<Status, String> future = service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
//        future.onSuccess(TestCallbacks.exceptionCallback(""));
//        latch.countDown();
//
//        int i = 0;
//        while (true) {
//            try {
//                service.submit(TestActions.successAction(0), 100L);
//                break;
//            } catch (RejectedActionException e) {
//                Thread.sleep(5);
//                if (i == 20) {
//                    fail("Continue to receive action rejects.");
//                }
//            }
//            ++i;
//        }
//    }
//
//    @Test
//    public void circuitBreaker() throws Exception {
//        BreakerConfigBuilder builder = new BreakerConfigBuilder();
//        builder.trailingPeriodMillis = 10000;
//        builder.failureThreshold = 5;
//        builder.backOffTimeMillis = 50;
//        // A hack to ensure that health is always refreshed.
//        builder.healthRefreshMillis = -1;
//
//        ActionMetrics<Status> metrics = new DefaultActionMetrics<>(Status.class, 3600, 1, TimeUnit.SECONDS);
//        CircuitBreaker breaker = new DefaultCircuitBreaker(builder.build());
//        ServiceProperties properties = new ServiceProperties();
//        properties.actionMetrics(metrics);
//        properties.circuitBreaker(breaker);
//        properties.concurrencyLevel(100);
//        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
//        service = Services.defaultService("Test", 1, properties);
//
//        List<PrecipiceFuture<Status, String>> fs = new ArrayList<>();
//        for (int i = 0; i < 6; ++i) {
//            fs.add(service.submit(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE));
//        }
//
//        for (PrecipiceFuture<Status, String> f : fs) {
//            try {
//                f.get();
//            } catch (ExecutionException e) {
//            }
//        }
//
//        Thread.sleep(10);
//
//        try {
//            service.submit(TestActions.successAction(0), 100);
//            fail("Should have been rejected due to open circuit.");
//        } catch (RejectedActionException e) {
//            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
//        }
//
//        Thread.sleep(150);
//
//        PrecipiceFuture<Status, String> f = service.submit(TestActions.successAction(0, "Result"), 100);
//        assertEquals("Result", f.get());
//
//        PrecipiceFuture<Status, String> fe = service.submit(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE);
//        try {
//            fe.get();
//        } catch (ExecutionException e) {
//        }
//
//        Thread.sleep(10);
//
//        try {
//            service.submit(TestActions.successAction(0), 100);
//            fail("Should have been rejected due to open circuit.");
//        } catch (RejectedActionException e) {
//            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
//        }
//    }
//
//    private static void assertNewMetrics(ActionMetrics<Status> metrics, Map<Object, Integer> expectedCounts) {
//        int milliseconds = 5;
//        int expectedErrors = expectedCounts.get(Status.ERROR) == null ? 0 : expectedCounts.get(Status.ERROR);
//        int expectedSuccesses = expectedCounts.get(Status.SUCCESS) == null ? 0 : expectedCounts.get(Status.SUCCESS);
//        int expectedTimeouts = expectedCounts.get(Status.TIMEOUT) == null ? 0 : expectedCounts.get(Status.TIMEOUT);
//        int expectedMaxConcurrency = expectedCounts.get(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED) == null ? 0 :
//                expectedCounts.get(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
//        int expectedCircuitOpen = expectedCounts.get(Rejected.CIRCUIT_OPEN) == null ? 0 :
//                expectedCounts.get(Rejected.CIRCUIT_OPEN);
//
//        assertEquals(expectedErrors, metrics.getMetricCountForTimePeriod(Status.ERROR, milliseconds, TimeUnit.SECONDS));
//        assertEquals(expectedTimeouts, metrics.getMetricCountForTimePeriod(Status.TIMEOUT, milliseconds, TimeUnit.SECONDS));
//        assertEquals(expectedSuccesses, metrics.getMetricCountForTimePeriod(Status.SUCCESS, milliseconds, TimeUnit.SECONDS));
//        assertEquals(expectedMaxConcurrency, metrics.getRejectionCountForTimePeriod(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED,
//                milliseconds, TimeUnit.SECONDS));
//        assertEquals(expectedCircuitOpen, metrics.getRejectionCountForTimePeriod(Rejected.CIRCUIT_OPEN, milliseconds, TimeUnit.SECONDS));
//    }

}
