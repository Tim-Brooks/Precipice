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

package net.uncontended.precipice.timeout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DelayQueueTimeoutServiceTest {

    private volatile boolean didTimeout;
    private DelayQueueTimeoutService timeoutService;
    private ConcurrentLinkedQueue<Integer> queue;

    @Before
    public void setUp() {
        didTimeout = false;
        timeoutService = new DelayQueueTimeoutService("Test-Timeout-Service");
        queue = new ConcurrentLinkedQueue<>();
    }

    @After
    public void tearDown() {
        timeoutService.stop();
    }

    @Test
    public void timeoutWillOccur() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        timeoutService.scheduleTimeout(new TestTimeout(latch), 10L);

        latch.await();

        assertTrue(didTimeout);
    }

    @Test
    public void timeoutsWillOccurInOrder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        timeoutService.scheduleTimeout(new TestTimeout2(latch, 3), 100L);
        timeoutService.scheduleTimeout(new TestTimeout2(latch, 1), 10L);
        timeoutService.scheduleTimeout(new TestTimeout2(latch, 2), 50L);

        latch.await();

        assertEquals(1, queue.poll().intValue());
        assertEquals(2, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
    }

    @Test
    public void timeoutsCannotBeSubmittedAfterServiceStopped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        timeoutService.stop();

        try {
            timeoutService.scheduleTimeout(new TestTimeout(latch), 1L);
            fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            assertEquals("Service has been stopped.", e.getMessage());
        }
    }

    private class TestTimeout implements Timeout {

        private final CountDownLatch latch;

        private TestTimeout(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void timeout() {
            didTimeout = true;
            latch.countDown();
        }
    }

    private class TestTimeout2 implements Timeout {

        private final CountDownLatch latch;
        private final int value;

        private TestTimeout2(CountDownLatch latch, int value) {
            this.latch = latch;
            this.value = value;
        }

        @Override
        public void timeout() {
            queue.add(value);
            latch.countDown();
        }
    }
}
