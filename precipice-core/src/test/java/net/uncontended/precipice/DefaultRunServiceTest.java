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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.test_utils.TestActions;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefaultRunServiceTest {

    private RunService service;
    private final ExecutorService actionRunner = Executors.newCachedThreadPool();

    @Before
    public void setUp() {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(100);
        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
        service = Services.runService("Test", properties);
    }

    @After
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void actionRejectedIfShutdown() throws Exception {
        service.shutdown();

        try {
            service.run(TestActions.successAction(0));
            fail("Exception should have been thrown due to shutdown.");
        } catch (IllegalStateException e) {
            assertEquals("Service has been shutdown.", e.getMessage());
        }
    }

    @Test
    public void actionNotScheduledIfMaxConcurrencyLevelViolated() throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(2);
        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
        service = Services.runService("Test", properties);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch startedLatch = new CountDownLatch(2);

        asyncRunAction(TestActions.blockedAction(latch), startedLatch);
        asyncRunAction(TestActions.blockedAction(latch), startedLatch);

        try {
            startedLatch.await();
            service.run(TestActions.successAction(1));
            fail();
        } catch (RejectedActionException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        try {
            service.run(TestActions.successAction(1));
            fail();
        } catch (RejectedActionException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        latch.countDown();
    }

    @Test
    public void actionsReleaseSemaphorePermitWhenComplete() throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(1);
        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
        service = Services.runService("Test", properties);
        int iterations = new Random().nextInt(50);
        for (int i = 0; i < iterations; ++i) {
            try {
                service.run(TestActions.successAction(1));
            } catch (RejectedActionException e) {
                fail("Continue to receive action rejects.");
            }
        }
    }

    @Test
    public void actionIsRun() throws Exception {
        String result = service.run(TestActions.successAction(1));
        assertEquals("Success", result);
    }

    @Test
    public void actionTimeoutExceptionWillBeConsideredTimeout() throws Exception {
        ActionTimeoutException exception = new ActionTimeoutException();
        try {
            String run = service.run(TestActions.erredAction(exception));
            fail("Not ActionTimeoutException thrown");
        } catch (ActionTimeoutException e) {
        }

        ActionMetrics<Status> actionMetrics = (ActionMetrics<Status>) service.getActionMetrics();
        assertEquals(1, actionMetrics.getMetricCountForTimePeriod(Status.TIMEOUT, 1, TimeUnit.HOURS));
    }

    @Test
    public void erredActionWillThrowException() {
        RuntimeException exception = new RuntimeException();

        try {
            service.run(TestActions.erredAction(exception));
            fail("No exception thrown.");
        } catch (Exception e) {
        }
    }

    @Test
    public void resultMetricsUpdated() throws Exception {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(3);

        try {
            service.run(TestActions.erredAction(new IOException()));
        } catch (IOException e) {
        }
        try {
            service.run(TestActions.erredAction(new ActionTimeoutException()));
        } catch (ActionTimeoutException e) {
        }
        service.run(TestActions.successAction(50, "Success"));

        ActionMetrics<Status> metrics = (ActionMetrics<Status>) service.getActionMetrics();
        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(Status.ERROR, 1);
        expectedCounts.put(Status.TIMEOUT, 1);


        assertNewMetrics(metrics, expectedCounts);
    }

    @Test
    public void rejectedMetricsUpdated() throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(1);
        properties.actionMetrics(new DefaultActionMetrics<Status>(Status.class));
        service = Services.runService("Test", properties);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch startedLatch = new CountDownLatch(1);
        asyncRunAction(TestActions.blockedAction(latch), startedLatch);

        startedLatch.await();

        try {
            service.run(TestActions.successAction(1));
        } catch (RejectedActionException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        latch.countDown();

        service.getCircuitBreaker().forceOpen();

        int i = 0;
        while (true) {
            if (service.pendingCount() == 0) {
                break;
            } else if (i == 10) {
                fail("Unexpected number of pending actions on service.");
            }
            Thread.sleep(50);
            ++i;
        }

        try {
            service.run(TestActions.successAction(1));
        } catch (RejectedActionException e) {
            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);

        }

        Map<Object, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(Status.SUCCESS, 1);
        expectedCounts.put(Rejected.CIRCUIT_OPEN, 1);
        expectedCounts.put(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1);

        assertNewMetrics((ActionMetrics<Status>) service.getActionMetrics(), expectedCounts);
    }

    @Test
    public void circuitBreaker() throws Exception {
        BreakerConfigBuilder builder = new BreakerConfigBuilder();
        builder.trailingPeriodMillis = 10000;
        builder.failureThreshold = 5;
        builder.backOffTimeMillis = 50;
        // A hack to ensure that health is always refreshed.
        builder.healthRefreshMillis = -1;

        ActionMetrics<Status> metrics = new DefaultActionMetrics<>(Status.class, 3600, 1, TimeUnit.SECONDS);
        CircuitBreaker breaker = new DefaultCircuitBreaker(builder.build());
        ServiceProperties properties = new ServiceProperties();
        properties.actionMetrics(metrics);
        properties.circuitBreaker(breaker);
        properties.concurrencyLevel(100);
        service = Services.runService("Test", properties);

        for (int i = 0; i < 6; ++i) {
            try {
                service.run(TestActions.erredAction(new RuntimeException()));
            } catch (RuntimeException e) {
            }
        }

        try {
            service.run(TestActions.successAction(0));
            fail("Should have been rejected due to open circuit.");
        } catch (RejectedActionException e) {
            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
        }

        Thread.sleep(150);

        String result = service.run(TestActions.successAction(0, "Result"));
        assertEquals("Result", result);

        try {
            service.run(TestActions.erredAction(new RuntimeException()));
        } catch (RuntimeException e) {
        }

        try {
            service.run(TestActions.successAction(0));
            fail("Should have been rejected due to open circuit.");
        } catch (RejectedActionException e) {
            assertEquals(Rejected.CIRCUIT_OPEN, e.reason);
        }
    }

    private void asyncRunAction(final ResilientAction<?> action, final CountDownLatch startedLatch) {
        actionRunner.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    service.run(new ResilientAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            startedLatch.countDown();
                            return action.run();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void assertNewMetrics(ActionMetrics<Status> metrics, Map<Object, Integer> expectedCounts) {
        int milliseconds = 5;
        int expectedErrors = expectedCounts.get(Status.ERROR) == null ? 0 : expectedCounts.get(Status.ERROR);
        int expectedSuccesses = expectedCounts.get(Status.SUCCESS) == null ? 0 : expectedCounts.get(Status.SUCCESS);
        int expectedTimeouts = expectedCounts.get(Status.TIMEOUT) == null ? 0 : expectedCounts.get(Status.TIMEOUT);
        int expectedMaxConcurrency = expectedCounts.get(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED) == null ? 0 :
                expectedCounts.get(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        int expectedCircuitOpen = expectedCounts.get(Rejected.CIRCUIT_OPEN) == null ? 0 :
                expectedCounts.get(Rejected.CIRCUIT_OPEN);

        assertEquals(expectedErrors, metrics.getMetricCountForTimePeriod(Status.ERROR, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedTimeouts, metrics.getMetricCountForTimePeriod(Status.TIMEOUT, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedSuccesses, metrics.getMetricCountForTimePeriod(Status.SUCCESS, milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedMaxConcurrency, metrics.getRejectionCountForTimePeriod(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED,
                milliseconds, TimeUnit.SECONDS));
        assertEquals(expectedCircuitOpen, metrics.getRejectionCountForTimePeriod(Rejected.CIRCUIT_OPEN,
                milliseconds, TimeUnit.SECONDS));
    }
}
