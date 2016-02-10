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

package net.uncontended.precipice.threadpool.experimental;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public class ActionThreadPool implements Executor {

    private final NavigableSet<ThreadManager> pool;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public ActionThreadPool(String actionName, int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Cannot have fewer than 1 thread");
        }
        pool = new TreeSet<>(new Comparator<ThreadManager>() {
            @Override
            public int compare(ThreadManager o1, ThreadManager o2) {
                int scheduledCount1 = o1.getScheduledCount();
                int scheduledCount2 = o2.getScheduledCount();
                if (scheduledCount1 > scheduledCount2) {
                    return 1;
                } else if (scheduledCount2 > scheduledCount1) {
                    return -1;
                } else if (random.nextBoolean()) {
                    return 1;
                } else {
                    return -1;
                }

            }
        });

        for (int i = 0; i < threadCount; ++i) {
            pool.add(new ThreadManager(actionName + "-" + i));
        }

    }

    @Override
    public void execute(Runnable action) {
        ThreadManager nextThread = pool.pollFirst();
        boolean submitted = nextThread.submit(action);
        pool.add(nextThread);
        if (!submitted) {
            throw new RejectedExecutionException();
        }
    }

    public void signalTaskComplete(ThreadManager threadManager) {
        threadManager.decrementScheduledCount();
        pool.remove(threadManager);
        pool.add(threadManager);
    }

    public void shutdown() {
        for (ThreadManager manager : pool) {
            manager.shutdown();
        }
    }

    private static class ThreadManager {
        private final ExchangingQueue<Runnable> queue = new ExchangingQueue<>(10);
        private final Thread thread;
        private int scheduledCount = 0;

        private ThreadManager(String threadName) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (; ; ) {
                        Runnable runnable;
                        try {
                            runnable = queue.blockingPoll();
                        } catch (InterruptedException e) {
                            return;
                        }
                        runnable.run();
                        // Need to explore this strategy more.
                        if (thread.isInterrupted()) {
                            return;
                        }
                    }
                }
            }, threadName);
            thread.start();
        }

        private boolean submit(Runnable task) {
            boolean offered = queue.offer(task);
            if (offered) {
                ++scheduledCount;
            }
            return offered;
        }

        private void decrementScheduledCount() {
            --scheduledCount;
        }

        private int getScheduledCount() {
            return scheduledCount;
        }

        private void shutdown() {
            thread.interrupt();
            LockSupport.unpark(thread);
        }
    }
}
