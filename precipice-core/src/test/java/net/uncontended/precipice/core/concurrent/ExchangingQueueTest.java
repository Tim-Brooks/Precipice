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

package net.uncontended.precipice.core.concurrent;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

public class ExchangingQueueTest {

    private ExchangingQueue<Integer> exchangingQueue;

    @Before
    public void setUp() {
        exchangingQueue = new ExchangingQueue<>(10);
    }

    @Test
    public void testPollReturnsNullWhenQueueEmpty() {
        assertNull(exchangingQueue.poll());

        assertTrue(exchangingQueue.offer(1));

        assertNotNull(exchangingQueue.poll());
        assertNull(exchangingQueue.poll());
    }

    @Test
    public void testOfferAddsToTailAndPollRemovesFromHead() {
        exchangingQueue.offer(1);
        exchangingQueue.offer(2);
        exchangingQueue.offer(3);

        assertEquals(Integer.valueOf(1), exchangingQueue.poll());
    }

    @Test
    public void testOfferReturnsFalseIfNoSpace() {
        exchangingQueue = new ExchangingQueue<>(1);

        assertTrue(exchangingQueue.offer(1));
        assertFalse(exchangingQueue.offer(2));
    }

    @Test
    public void testOfferWillWrapAroundArray() {
        exchangingQueue = new ExchangingQueue<>(1);

        assertTrue(exchangingQueue.offer(1));
        assertEquals(Integer.valueOf(1), exchangingQueue.poll());
        assertTrue(exchangingQueue.offer(2));
    }

    @Test
    public void testQueueEndToEnd() throws Exception {
        final List<Integer> events = new ArrayList<>();
        final ExchangingQueue<Integer> exchangingQueue = new ExchangingQueue<>(100);

        Random random = new Random();
        int count = random.nextInt(1000) + 1500;

        for (int i = 0; i < count; ++i) {
            events.add(random.nextInt());
        }

        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Integer event : events) {
                    for (; ; ) {
                        if (!exchangingQueue.offer(event)) {
                            LockSupport.parkNanos(1);
                        } else {
                            break;
                        }
                    }
                }
            }
        });
        producer.start();

        int receivedCount = 0;
        List<Integer> received = new ArrayList<>();

        while (count != receivedCount) {
            Integer poll = exchangingQueue.blockingPoll();
            received.add(poll);
            ++receivedCount;
        }

        for (int i = 0; i < received.size(); ++i) {
            assertEquals(events.get(i), received.get(i));
        }

    }
}
