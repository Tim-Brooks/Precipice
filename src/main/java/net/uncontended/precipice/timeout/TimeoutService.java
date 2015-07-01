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

package net.uncontended.precipice.timeout;

import net.uncontended.precipice.concurrent.ResilientTask;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeoutService {

    public static final TimeoutService defaultTimeoutService = new TimeoutService("default");
    private final DelayQueue<ResilientTask<?>> timeoutQueue = new DelayQueue<>();
    private final Thread timeoutThread;
    private AtomicBoolean isStarted = new AtomicBoolean(false);

    public TimeoutService(String name) {
        this.timeoutThread = createThread();
        this.timeoutThread.setName(name + "-timeout-thread");
        // TODO: Determine correct strategy for shutting down timeout service.
        this.timeoutThread.setDaemon(true);
    }

    public void scheduleTimeout(ResilientTask<?> task) {
        if (!isStarted.get()) {
            startThread();
        }
        timeoutQueue.offer(task);
    }

    private void startThread() {
        if (isStarted.compareAndSet(false, true)) {
            timeoutThread.start();
        }
    }

    private Thread createThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    try {
                        ResilientTask<?> task = timeoutQueue.take();
                        task.setTimedOut();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

}
