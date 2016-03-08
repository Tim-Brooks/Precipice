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
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeoutService {

    public static final long MAX_TIMEOUT_MILLIS = 1000 * 60 * 60 * 24;
    public static final TimeoutService DEFAULT_TIMEOUT_SERVICE = new TimeoutService("default");

    private final DelayQueue<TimeoutHolder> timeoutQueue = new DelayQueue<>();
    private final Thread timeoutThread;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    public TimeoutService(String name) {
        timeoutThread = createThread();
        timeoutThread.setName(name + "-timeout-thread");
        // TODO: Determine correct strategy for shutting down timeout service.
        timeoutThread.setDaemon(true);
    }

    public void scheduleTimeout(net.uncontended.precipice.timeout.Timeout task, long timeoutMillis) {
        scheduleTimeout(task, timeoutMillis, System.nanoTime());
    }

    public void scheduleTimeout(net.uncontended.precipice.timeout.Timeout task, long timeoutMillis, long nanoTime) {
        if (!isStarted.get()) {
            startThread();
        }

        timeoutQueue.offer(new TimeoutHolder(task, timeoutMillis, nanoTime));
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
                        TimeoutHolder task = timeoutQueue.take();
                        task.setTimedOut();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

    private static class TimeoutHolder implements Delayed {

        private final net.uncontended.precipice.timeout.Timeout task;
        public final long nanosAbsoluteTimeout;
        public final long millisRelativeTimeout;

        public TimeoutHolder(net.uncontended.precipice.timeout.Timeout task, long millisRelativeTimeout, long nanosAbsoluteStart) {
            this.task = task;
            this.millisRelativeTimeout = millisRelativeTimeout;
            nanosAbsoluteTimeout = TimeUnit.NANOSECONDS.convert(millisRelativeTimeout, TimeUnit.MILLISECONDS) + nanosAbsoluteStart;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(nanosAbsoluteTimeout - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (o instanceof TimeoutHolder) {
                return Long.compare(nanosAbsoluteTimeout, ((TimeoutHolder) o).nanosAbsoluteTimeout);
            }
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
        }

        public void setTimedOut() {
            task.timeout();
        }
    }
}
