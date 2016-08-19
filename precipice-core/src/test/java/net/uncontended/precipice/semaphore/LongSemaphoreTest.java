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

package net.uncontended.precipice.semaphore;

import net.uncontended.precipice.rejected.Rejected;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.*;

public class LongSemaphoreTest {

    private final Executor executor = Executors.newFixedThreadPool(4);
    private LongSemaphore<Rejected> semaphore;
    private volatile long concurrencyLevel;

    @Test
    public void semaphoreAllowsExpectedNumberOfActions() throws InterruptedException {
        concurrencyLevel = ThreadLocalRandom.current().nextLong(15) + 1;
        semaphore = new LongSemaphore<>(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, concurrencyLevel);

        final AtomicBoolean isFailed = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch((int) concurrencyLevel);
        for (int i = 0; i < concurrencyLevel; ++i) {
            int finalI = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Rejected rejected = semaphore.acquirePermit(1, finalI);
                    if (rejected != null) {
                        isFailed.set(true);
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertFalse(isFailed.get());

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, semaphore.acquirePermit(1, 100L));
        assertEquals(0, semaphore.remainingCapacity());
        assertEquals(concurrencyLevel, semaphore.maxConcurrencyLevel());
        assertEquals(concurrencyLevel, semaphore.currentConcurrencyLevel());
    }

    @Test
    public void semaphoreSupportsMultiplePermits() throws InterruptedException {
        concurrencyLevel = 15;
        semaphore = new LongSemaphore<>(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, concurrencyLevel);

        final AtomicBoolean isFailed = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(7);
        for (int i = 0; i < 7; ++i) {
            int finalI = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Rejected rejected = semaphore.acquirePermit(2, finalI);
                    if (rejected != null) {
                        isFailed.set(true);
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertFalse(isFailed.get());

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, semaphore.acquirePermit(2, 100L));
        assertEquals(1, semaphore.remainingCapacity());
        assertEquals(15, semaphore.maxConcurrencyLevel());
        assertEquals(14, semaphore.currentConcurrencyLevel());
        assertNull(semaphore.acquirePermit(1, 100L));
        assertEquals(15, semaphore.currentConcurrencyLevel());
    }

    @Test
    public void semaphoreSupportsReleasingPermits() throws InterruptedException {
        concurrencyLevel = 8;
        semaphore = new LongSemaphore<>(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, concurrencyLevel);

        final AtomicBoolean isFailed = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; ++i) {
            int finalI = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Rejected rejected = semaphore.acquirePermit(2, finalI);
                    if (rejected != null) {
                        isFailed.set(true);
                    }
                    semaphore.releasePermit(2, finalI + 1);
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertFalse(isFailed.get());

        assertEquals(8, semaphore.remainingCapacity());
        assertEquals(8, semaphore.maxConcurrencyLevel());
        assertEquals(0, semaphore.currentConcurrencyLevel());
    }
}
