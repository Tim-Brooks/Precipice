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

import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.concurrent.DefaultResilientPromise;
import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.concurrent.ResilientPromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.Metric;
import net.uncontended.precipice.test_utils.TestActions;
import net.uncontended.precipice.test_utils.TestCallbacks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class DefaultServiceTest {

    private MultiService blockingExecutor;

    @Before
    public void setUp() {
        blockingExecutor = Services.defaultService("Test", 1, 30);
    }

    @After
    public void tearDown() {
        blockingExecutor.shutdown();
    }

    @Test
    public void actionRejectedIfShutdown() {
        blockingExecutor.shutdown();

        try {
            blockingExecutor.submitAction(TestActions.successAction(0), Long.MAX_VALUE);
            fail("Action should have been rejected due to shutdown.");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.SERVICE_SHUTDOWN, e.reason);
        }
    }

    @Test
    public void actionNotScheduledIfMaxConcurrencyLevelViolated() throws Exception {
        blockingExecutor = Services.defaultService("Test", 1, 2);
        CountDownLatch latch = new CountDownLatch(1);
        blockingExecutor.submitAction(TestActions.blockedAction(latch), Long.MAX_VALUE);
        blockingExecutor.submitAction(TestActions.blockedAction(latch), Long.MAX_VALUE);

        try {
            blockingExecutor.submitAction(TestActions.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        try {
            blockingExecutor.performAction(TestActions.successAction(1));
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        latch.countDown();
    }

    @Test
    public void actionsReleaseSemaphorePermitWhenComplete() throws Exception {
        blockingExecutor = Services.defaultService("Test", 1, 1);
        int iterations = new Random().nextInt(50);
        for (int i = 0; i < iterations; ++i) {
            ResilientFuture<String> future = blockingExecutor.submitAction(TestActions.successAction(1), 500);
            future.get();
            int j = 0;
            while (true) {
                try {
                    blockingExecutor.performAction(TestActions.successAction(1));
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
        ResilientFuture<String> f = blockingExecutor.submitAction(TestActions.successAction(1), 500);

        assertEquals("Success", f.get());
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void futureIsPendingUntilSubmittedActionFinished() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ResilientFuture<String> f = blockingExecutor.submitAction(TestActions.blockedAction(latch), Long.MAX_VALUE);
        assertEquals(Status.PENDING, f.getStatus());
        latch.countDown();
        f.get();
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void performActionCompletesAction() throws Exception {
        String result = blockingExecutor.performAction(TestActions.successAction(1));
        assertEquals("Success", result);
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        DefaultResilientPromise<String> promise = new DefaultResilientPromise<>();
        blockingExecutor.submitAction(TestActions.successAction(50, "Same Promise"), promise, Long.MAX_VALUE);

        assertEquals("Same Promise", promise.awaitResult());

    }

    @Test
    public void promiseWillNotBeCompletedTwice() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        DefaultResilientPromise<String> promise = new DefaultResilientPromise<>();

        blockingExecutor.submitAction(TestActions.blockedAction(latch), promise, Long.MAX_VALUE);

        promise.deliverResult("CompleteOnThisThread");
        latch.countDown();

        for (int i = 0; i < 10; ++i) {
            Thread.sleep(5);
            assertEquals("CompleteOnThisThread", promise.awaitResult());
        }
    }

    @Test
    public void submittedActionWillTimeout() throws Exception {
        ResilientFuture<String> future = blockingExecutor.submitAction(TestActions.blockedAction(new CountDownLatch
                (1)), 1);

        assertNull(future.get());
        assertEquals(Status.TIMEOUT, future.getStatus());
    }

    @Test
    public void erredActionWillReturnedException() {
        RuntimeException exception = new RuntimeException();
        ResilientFuture<String> future = blockingExecutor.submitAction(TestActions.erredAction(exception), 100);

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
        ResilientPromise<ResilientPromise<String>> errorPromise = new DefaultResilientPromise<>();
        ResilientPromise<ResilientPromise<String>> successPromise = new DefaultResilientPromise<>();
        ResilientPromise<ResilientPromise<String>> timeOutPromise = new DefaultResilientPromise<>();


        CountDownLatch blockingLatch = new CountDownLatch(1);

        ResilientFuture<String> errorF = blockingExecutor.submitAction(TestActions.erredAction(new IOException()),
                TestCallbacks.completePromiseCallback(errorPromise), 100);
        ResilientFuture<String> timeOutF = blockingExecutor.submitAction(TestActions.blockedAction(blockingLatch),
                TestCallbacks.completePromiseCallback(timeOutPromise), 1);
        ResilientFuture<String> successF = blockingExecutor.submitAction(TestActions.successAction(50, "Success"),
                TestCallbacks.completePromiseCallback(successPromise), Long.MAX_VALUE);

        errorPromise.await();
        successPromise.await();
        timeOutPromise.await();
        blockingLatch.countDown();

        assertEquals(errorF.promise, errorPromise.getResult());
        assertEquals(successF.promise, successPromise.getResult());
        assertEquals(timeOutF.promise, timeOutPromise.getResult());
    }

    @Test
    public void resultMetricsUpdated() throws Exception {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(3);

        ResilientCallback<String> countdownCallback = TestCallbacks.latchedCallback(blockingLatch);
        ResilientFuture<String> errorF = blockingExecutor.submitAction(TestActions.erredAction(new IOException()),
                countdownCallback, 100);
        ResilientFuture<String> timeOutF = blockingExecutor.submitAction(TestActions.blockedAction(timeoutLatch),
                countdownCallback, 1);
        ResilientFuture<String> successF = blockingExecutor.submitAction(TestActions.successAction(50, "Success"),
                countdownCallback, Long.MAX_VALUE);

        for (ResilientFuture<String> f : Arrays.asList(errorF, timeOutF, successF)) {
            try {
                f.get();
                f.get();
                f.get();
            } catch (ExecutionException e) {
            }
        }

        ActionMetrics metrics = blockingExecutor.getActionMetrics();
        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(Status.ERROR, 1);
        expectedCounts.put(Status.TIMEOUT, 1);

        blockingLatch.await();

        assertNewMetrics(metrics, expectedCounts);
    }

    @Test
    public void rejectedMetricsUpdated() throws Exception {
        blockingExecutor = Services.defaultService("Test", 1, 1);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(1);
        ResilientCallback<String> callback = TestCallbacks.latchedCallback(blockingLatch);

        ResilientFuture<String> f = blockingExecutor.submitAction(TestActions.blockedAction(latch), callback,
                Long.MAX_VALUE);

        try {
            blockingExecutor.submitAction(TestActions.successAction(1), callback, Long.MAX_VALUE);
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        latch.countDown();
        f.get();
        blockingLatch.await();
        blockingExecutor.getCircuitBreaker().forceOpen();

        int maxConcurrencyErrors = 1;
        for (int i = 0; i < 5; ++i) {
            try {
                blockingExecutor.submitAction(TestActions.successAction(1), Long.MAX_VALUE);
            } catch (RejectedActionException e) {
                if (e.reason == RejectionReason.CIRCUIT_OPEN) {
                    break;
                } else {
                    maxConcurrencyErrors++;
                }
            }
        }

        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(RejectionReason.CIRCUIT_OPEN, 1);
        expectedCounts.put(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, maxConcurrencyErrors);

        assertNewMetrics(blockingExecutor.getActionMetrics(), expectedCounts);
    }

    @Test
    public void metricsUpdatedEvenIfPromiseAlreadyCompleted() throws Exception {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(3);
        ResilientPromise<String> errP = new DefaultResilientPromise<>();
        ResilientPromise<String> timeoutP = new DefaultResilientPromise<>();
        ResilientPromise<String> successP = new DefaultResilientPromise<>();
        ResilientCallback<String> callback = TestCallbacks.latchedCallback(blockingLatch);
        errP.deliverResult("Done");
        timeoutP.deliverResult("Done");
        successP.deliverResult("Done");

        blockingExecutor.submitAction(TestActions.erredAction(new IOException()), errP, callback, 100);
        blockingExecutor.submitAction(TestActions.blockedAction(timeoutLatch), timeoutP, callback, 1);
        blockingExecutor.submitAction(TestActions.successAction(50, "Success"), successP, callback, Long.MAX_VALUE);

        ActionMetrics metrics = blockingExecutor.getActionMetrics();
        for (int i = 0; i < 9; ++i) {
            if (metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 5, TimeUnit.SECONDS) == 1) {
                break;
            } else {
                if (i == 10) {
                    fail("Never encountered a timeout.");
                } else {
                    Thread.sleep(5);
                }
            }
        }

        timeoutLatch.countDown();

        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(Status.ERROR, 1);
        expectedCounts.put(Status.TIMEOUT, 1);

        blockingLatch.await();
        assertNewMetrics(metrics, expectedCounts);
    }

    @Test
    public void semaphoreReleasedDespiteCallbackException() throws Exception {
        blockingExecutor = Services.defaultService("Test", 1, 1);
        blockingExecutor.submitAction(TestActions.successAction(0), TestCallbacks.exceptionCallback(""), Long.MAX_VALUE);

        int i = 0;
        while (true) {
            try {
                blockingExecutor.performAction(TestActions.successAction(0));
                break;
            } catch (RejectedActionException e) {
                Thread.sleep(5);
                if (i == 20) {
                    fail("Continue to receive action rejects.");
                }
            }
            ++i;
        }

    }

    @Test
    public void circuitBreaker() throws Exception {
        BreakerConfigBuilder builder = new BreakerConfigBuilder();
        builder.trailingPeriodMillis = 10000;
        builder.failureThreshold = 5;
        builder.backOffTimeMillis = 50;

        ActionMetrics metrics = new DefaultActionMetrics(3600, 1, TimeUnit.SECONDS);
        CircuitBreaker breaker = new DefaultCircuitBreaker(metrics, builder.build());
        blockingExecutor = Services.defaultService("Test", 1, 100, metrics, breaker);

        List<ResilientFuture<String>> fs = new ArrayList<>();
        for (int i = 0; i < 6; ++i) {
            fs.add(blockingExecutor.submitAction(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE));
        }

        for (ResilientFuture<String> f : fs) {
            try {
                f.get();
            } catch (ExecutionException e) {
            }
        }

        Thread.sleep(10);

        try {
            blockingExecutor.submitAction(TestActions.successAction(0), 100);
            fail("Should have been rejected due to open circuit.");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.CIRCUIT_OPEN, e.reason);
        }

        Thread.sleep(150);

        ResilientFuture<String> f = blockingExecutor.submitAction(TestActions.successAction(0, "Result"), 100);
        assertEquals("Result", f.get());

        ResilientFuture<String> fe = blockingExecutor.submitAction(TestActions.erredAction(new RuntimeException()), Long.MAX_VALUE);
        try {
            fe.get();
        } catch (ExecutionException e) {
        }

        Thread.sleep(10);

        try {
            blockingExecutor.submitAction(TestActions.successAction(0), 100);
            fail("Should have been rejected due to open circuit.");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.CIRCUIT_OPEN, e.reason);
        }
    }

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
