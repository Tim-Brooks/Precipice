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

package net.uncontended.precipice.core;

import net.uncontended.precipice.core.concurrent.Eventual;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.Promise;
import net.uncontended.precipice.core.metrics.ActionMetrics;
import net.uncontended.precipice.core.metrics.Metric;
import net.uncontended.precipice.core.test_utils.TestActions;
import net.uncontended.precipice.core.test_utils.TestCallbacks;
import net.uncontended.precipice.core.timeout.ActionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class DefaultServiceTest {

    private MultiService service;

    @Before
    public void setUp() {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(100);
        service = Services.defaultService("Test", 1, properties);
    }

    @After
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void actionRejectedIfShutdown() {
        service.shutdown();

        try {
            service.submit(TestActions.successAction(0), Long.MAX_VALUE);
            fail("Action should have been rejected due to shutdown.");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.SERVICE_SHUTDOWN, e.reason);
        }
    }

    @Test
    public void actionNotScheduledIfMaxConcurrencyLevelViolated() throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(2);
        service = Services.defaultService("Test", 1, properties);
        CountDownLatch latch = new CountDownLatch(1);
        service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
        service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);

        try {
            service.submit(TestActions.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        try {
            service.run(TestActions.successAction(1));
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        latch.countDown();
    }

    @Test
    public void actionsReleaseSemaphorePermitWhenComplete() throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(1);
        service = Services.defaultService("Test", 1, properties);
        int iterations = new Random().nextInt(50);
        for (int i = 0; i < iterations; ++i) {
            PrecipiceFuture<String> future = service.submit(TestActions.successAction(1), 500);
            future.get();
            int j = 0;
            while (true) {
                try {
                    service.run(TestActions.successAction(1));
                    break;
                } catch (RejectedActionException e) {
                    Thread.sleep(5);
                    if (j == 20) {
                        fail("Continue to receive action rejects.");
                    }
                }
                ++j;
            }
        }
    }

    @Test
    public void actionIsSubmittedAndRan() throws Exception {
        PrecipiceFuture<String> f = service.submit(TestActions.successAction(1), 500);

        assertEquals("Success", f.get());
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void futureIsPendingUntilSubmittedActionFinished() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<String> f = service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
        assertEquals(Status.PENDING, f.getStatus());
        latch.countDown();
        f.get();
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void runCompletesAction() throws Exception {
        String result = service.run(TestActions.successAction(1));
        assertEquals("Success", result);
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        Promise<String> promise = new Eventual<>();
        service.complete(TestActions.successAction(50, "Same Promise"), promise, Long.MAX_VALUE);

        assertEquals("Same Promise", promise.future().get());

    }

    @Test
    public void promiseWillNotBeCompletedTwice() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Promise<String> promise = new Eventual<>();

        service.complete(TestActions.blockedAction(latch), promise, Long.MAX_VALUE);

        promise.complete("CompleteOnThisThread");
        latch.countDown();

        for (int i = 0; i < 10; ++i) {
            Thread.sleep(5);
            assertEquals("CompleteOnThisThread", promise.future().get());
        }
    }

    @Test
    public void submittedActionWillTimeout() throws Exception {
        PrecipiceFuture<String> future = service.submit(TestActions.blockedAction(new CountDownLatch
                (1)), 1);

        assertNull(future.get());

        assertEquals(Status.TIMEOUT, future.getStatus());
    }

    @Test
    public void actionTimeoutExceptionWillBeConsideredTimeout() throws Exception {
        ActionTimeoutException exception = new ActionTimeoutException();
        PrecipiceFuture<String> future = service.submit(TestActions.erredAction(exception), 100);

        assertNull(future.get());
        assertEquals(Status.TIMEOUT, future.getStatus());
    }

    @Test
    public void erredActionWillReturnException() {
        RuntimeException exception = new RuntimeException();
        PrecipiceFuture<String> future = service.submit(TestActions.erredAction(exception), 100);

        try {
            future.get();
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            assertEquals(exception, e.getCause());
        }
        assertEquals(Status.ERROR, future.getStatus());
    }

    @Test
    public void attachedCallbacksWillBeExecutedOnCompletion() throws Exception {
        // Replace with Futures test
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);


        CountDownLatch blockingLatch = new CountDownLatch(1);
        final CountDownLatch callbackLatch = new CountDownLatch(3);

        IOException exception = new IOException();
        PrecipiceFuture<String> errorF = service.submit(TestActions.erredAction(exception), 100);
        errorF.onError(new PrecipiceFunction<Throwable>() {
            @Override
            public void apply(Throwable argument) {
                error.set(argument);
                callbackLatch.countDown();
            }
        });

        PrecipiceFuture<String> timeOutF = service.submit(TestActions.blockedAction(blockingLatch), 1);
        timeOutF.onTimeout(new PrecipiceFunction<Void>() {
            @Override
            public void apply(Void argument) {
                isTimedOut.set(true);
                callbackLatch.countDown();
            }
        });

        String resultString = "Success";
        final PrecipiceFuture<String> successF = service.submit(TestActions.successAction(50, resultString), Long.MAX_VALUE);
        successF.onSuccess(new PrecipiceFunction<String>() {
            @Override
            public void apply(String argument) {
                result.set(argument);
                callbackLatch.countDown();
            }
        });

        callbackLatch.await();
        blockingLatch.countDown();

        assertSame(exception, error.get());
        assertSame(resultString, successF.get());
        assertTrue(isTimedOut.get());
    }

    @Test
    public void resultMetricsUpdated() throws Exception {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(3);

        PrecipiceFuture<String> errorF = service.submit(TestActions.erredAction(new IOException()), 100);
        PrecipiceFunction<Throwable> callback = TestCallbacks.latchedCallback(blockingLatch);
        errorF.onError(callback);
        PrecipiceFuture<String> timeOutF = service.submit(TestActions.blockedAction(timeoutLatch), 1);
        PrecipiceFunction<Void> callback2 = TestCallbacks.latchedCallback(blockingLatch);
        timeOutF.onTimeout(callback2);

        PrecipiceFuture<String> successF = service.submit(TestActions.successAction(50, "Success"), Long.MAX_VALUE);
        PrecipiceFunction<String> callback3 = TestCallbacks.latchedCallback(blockingLatch);
        successF.onSuccess(callback3);

        for (PrecipiceFuture<String> f : Arrays.asList(errorF, timeOutF, successF)) {
            try {
                f.get();
                f.get();
                f.get();
            } catch (ExecutionException e) {
            }
        }

        ActionMetrics metrics = service.getActionMetrics();
        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(Status.ERROR, 1);
        expectedCounts.put(Status.TIMEOUT, 1);

        blockingLatch.await();

        assertNewMetrics(metrics, expectedCounts);
    }

    //
//    @Test
//    public void rejectedMetricsUpdated() throws Exception {
//        ServiceProperties properties = new ServiceProperties();
//        properties.concurrencyLevel(1);
//        service = Services.defaultService("Test", 1, properties);
//        CountDownLatch latch = new CountDownLatch(1);
//        CountDownLatch blockingLatch = new CountDownLatch(1);
//        ResilientCallback<String> callback = TestCallbacks.latchedCallback(blockingLatch);
//
//        ResilientFuture<String> f = service.complete(TestActions.blockedAction(latch), callback,
//                Long.MAX_VALUE);
//
//        try {
//            service.complete(TestActions.successAction(1), callback, Long.MAX_VALUE);
//        } catch (RejectedActionException e) {
//            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
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
//                service.complete(TestActions.successAction(1), Long.MAX_VALUE);
//            } catch (RejectedActionException e) {
//                if (e.reason == RejectionReason.CIRCUIT_OPEN) {
//                    break;
//                } else {
//                    maxConcurrencyErrors++;
//                }
//            }
//        }
//
//        Map<Object, Integer> expectedCounts = new HashMap<>();
//        expectedCounts.put(Status.SUCCESS, 1);
//        expectedCounts.put(RejectionReason.CIRCUIT_OPEN, 1);
//        expectedCounts.put(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, maxConcurrencyErrors);
//
//        assertNewMetrics(service.getActionMetrics(), expectedCounts);
//    }
//
//    @Test
//    public void metricsUpdatedEvenIfPromiseAlreadyCompleted() throws Exception {
//        CountDownLatch timeoutLatch = new CountDownLatch(1);
//        CountDownLatch blockingLatch = new CountDownLatch(3);
//        ResilientPromise<String> errP = new DefaultResilientPromise<>();
//        ResilientPromise<String> timeoutP = new DefaultResilientPromise<>();
//        ResilientPromise<String> successP = new DefaultResilientPromise<>();
//        ResilientCallback<String> callback = TestCallbacks.latchedCallback(blockingLatch);
//        errP.deliverResult("Done");
//        timeoutP.deliverResult("Done");
//        successP.deliverResult("Done");
//
//        service.submitAndComplete(TestActions.erredAction(new IOException()), errP, callback, 100);
//        service.submitAndComplete(TestActions.blockedAction(timeoutLatch), timeoutP, callback, 1);
//        service.submitAndComplete(TestActions.successAction(50, "Success"), successP, callback, Long.MAX_VALUE);
//
//        ActionMetrics metrics = service.getActionMetrics();
//        for (int i = 0; i < 9; ++i) {
//            if (metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 5, TimeUnit.SECONDS) == 1) {
//                break;
//            } else {
//                if (i == 10) {
//                    fail("Never encountered a timeout.");
//                } else {
//                    Thread.sleep(5);
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
//        blockingLatch.await();
//        assertNewMetrics(metrics, expectedCounts);
//    }
//
//    @Test
//    public void semaphoreReleasedDespiteCallbackException() throws Exception {
//        ServiceProperties properties = new ServiceProperties();
//        properties.concurrencyLevel(1);
//        service = Services.defaultService("Test", 1, properties);
//        service.complete(TestActions.successAction(0), TestCallbacks.exceptionCallback(""), Long.MAX_VALUE);
//
//        int i = 0;
//        while (true) {
//            try {
//                service.run(TestActions.successAction(0));
//                break;
//            } catch (RejectedActionException e) {
//                Thread.sleep(5);
//                if (i == 20) {
//                    fail("Continue to receive action rejects.");
//                }
//            }
//            ++i;
//        }
//
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
//        ActionMetrics metrics = new DefaultActionMetrics(3600, 1, TimeUnit.SECONDS);
//        CircuitBreaker breaker = new DefaultCircuitBreaker(builder.build());
//        ServiceProperties properties = new ServiceProperties();
//        properties.actionMetrics(metrics);
//        properties.circuitBreaker(breaker);
//        properties.concurrencyLevel(100);
//        service = Services.defaultService("Test", 1, properties);
//
//        List<ResilientFuture<String>> fs = new ArrayList<>();
//        for (int i = 0; i < 6; ++i) {
//            fs.add(service.complete(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE));
//        }
//
//        for (ResilientFuture<String> f : fs) {
//            try {
//                f.get();
//            } catch (ExecutionException e) {
//            }
//        }
//
//        Thread.sleep(10);
//
//        try {
//            service.complete(TestActions.successAction(0), 100);
//            fail("Should have been rejected due to open circuit.");
//        } catch (RejectedActionException e) {
//            assertEquals(RejectionReason.CIRCUIT_OPEN, e.reason);
//        }
//
//        Thread.sleep(150);
//
//        ResilientFuture<String> f = service.complete(TestActions.successAction(0, "Result"), 100);
//        assertEquals("Result", f.get());
//
//        ResilientFuture<String> fe = service.complete(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE);
//        try {
//            fe.get();
//        } catch (ExecutionException e) {
//        }
//
//        Thread.sleep(10);
//
//        try {
//            service.complete(TestActions.successAction(0), 100);
//            fail("Should have been rejected due to open circuit.");
//        } catch (RejectedActionException e) {
//            assertEquals(RejectionReason.CIRCUIT_OPEN, e.reason);
//        }
//    }
//
    private void assertNewMetrics(ActionMetrics metrics, Map<Object, Integer> expectedCounts) {
        int milliseconds = 5;
        int expectedErrors = expectedCounts.get(Status.ERROR) == null ? 0 : expectedCounts.get(Status.ERROR);
        int expectedSuccesses = expectedCounts.get(Status.SUCCESS) == null ? 0 : expectedCounts.get(Status.SUCCESS);
        int expectedTimeouts = expectedCounts.get(Status.TIMEOUT) == null ? 0 : expectedCounts.get(Status.TIMEOUT);
        int expectedMaxConcurrency = expectedCounts.get(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED) == null ? 0 :
                expectedCounts.get(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        int expectedCircuitOpen = expectedCounts.get(RejectionReason.CIRCUIT_OPEN) == null ? 0 : expectedCounts.get
                (RejectionReason.CIRCUIT_OPEN);

        assertEquals(expectedErrors, metrics.getMetricCountForTimePeriod(Metric.ERROR, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedTimeouts, metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedSuccesses, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedMaxConcurrency, metrics.getMetricCountForTimePeriod(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedCircuitOpen, metrics.getMetricCountForTimePeriod(Metric.CIRCUIT_OPEN, milliseconds, TimeUnit.SECONDS));
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.QUEUE_FULL, milliseconds, TimeUnit.SECONDS));

    }
}
