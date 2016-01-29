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

import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeoutService {

    public static long MAX_TIMEOUT_MILLIS = 1000 * 60 * 60 * 24;

    public static final long NO_TIMEOUT = -1;
    public static final TimeoutService defaultTimeoutService = new TimeoutService("default");
    private final DelayQueue<TimeoutTask> timeoutQueue = new DelayQueue<>();
    private final Thread timeoutThread;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    public TimeoutService(String name) {
        timeoutThread = createThread();
        timeoutThread.setName(name + "-timeout-thread");
        // TODO: Determine correct strategy for shutting down timeout service.
        timeoutThread.setDaemon(true);
    }

    public void scheduleTimeout(TimeoutTask task) {
        if (!isStarted.get()) {
            startThread();
        }
        if (task.getMillisRelativeTimeout() != NO_TIMEOUT) {
            timeoutQueue.offer(task);
        }
    }

    public static long adjustTimeout(long millisTimeout) {
        return millisTimeout > MAX_TIMEOUT_MILLIS ? MAX_TIMEOUT_MILLIS : millisTimeout;
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
                        TimeoutTask task = timeoutQueue.take();
                        task.setTimedOut();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

}
