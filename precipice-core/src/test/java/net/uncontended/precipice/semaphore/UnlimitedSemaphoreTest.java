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

import net.uncontended.precipice.rejected.Unrejectable;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class UnlimitedSemaphoreTest {

    private final Executor executor = Executors.newCachedThreadPool();

    private final UnlimitedSemaphore<Unrejectable> semaphore = new UnlimitedSemaphore<>();

    @Test
    public void testSemaphore() throws InterruptedException {
        int acquireCount = ThreadLocalRandom.current().nextInt(20) + 5;
        CountDownLatch latch = new CountDownLatch(acquireCount);
        for (int i = 0; i < acquireCount; ++i) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    semaphore.acquirePermit(3, acquireCount);
                    latch.countDown();
                }
            });
        }
        latch.await();

        assertEquals(acquireCount * 3, semaphore.currentConcurrencyLevel());

        CountDownLatch latch2 = new CountDownLatch(acquireCount);
        for (int i = 0; i < acquireCount; ++i) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    semaphore.releasePermit(3, acquireCount);
                    latch2.countDown();
                }
            });
        }
        latch2.await();

        assertEquals(0, semaphore.currentConcurrencyLevel());
    }
}
