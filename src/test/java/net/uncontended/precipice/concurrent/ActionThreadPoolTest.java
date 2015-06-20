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

package net.uncontended.precipice.concurrent;

import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by timbrooks on 12/3/14.
 */
public class ActionThreadPoolTest {

    private ActionThreadPool threadPool;

    @After
    public void tearDown() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    @Test
    public void testPoolRequiresAtLeastOneThread() {
        try {
            threadPool = new ActionThreadPool("Test Action", 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot have fewer than 1 thread", e.getMessage());
        }
    }

    @Test
    public void testPoolPrioritizesFreeThreadsAndExecutes() {
        threadPool = new ActionThreadPool("Test Action", 2);

        final List<String> resultList = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 20; ++i) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    resultList.add(Thread.currentThread().getName());
                }
            });

        }

        while (resultList.size() != 20) {
        }

        int threadZeroCount = 0;
        int threadOneCount = 0;
        for (String threadName : resultList) {
            if ("Test Action-0".equals(threadName)) {
                ++threadZeroCount;
            }
            if ("Test Action-1".equals(threadName)) {
                ++threadOneCount;
            }
        }

        assertEquals(10, threadZeroCount);
        assertEquals(10, threadOneCount);

    }

    @Test
    public void signallyATaskCompleteFreesUpAThread() {
        threadPool = new ActionThreadPool("Test Action", 2);

        final List<String> resultList = new CopyOnWriteArrayList<>();
        Runnable action = new Runnable() {
            @Override
            public void run() {
                resultList.add(Thread.currentThread().getName());
            }
        };

        threadPool.execute(action);
        threadPool.execute(action);
        threadPool.execute(action);

        while (resultList.size() != 3) {
        }

    }
}
