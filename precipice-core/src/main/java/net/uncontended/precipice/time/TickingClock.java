/*
 * Copyright 2015 Timothy Brooks
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

package net.uncontended.precipice.time;

import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class TickingClock implements Clock {

    public Histogram gram = new Histogram(TimeUnit.MILLISECONDS.toNanos(100), 3);
    private static final AtomicReference<TickingClock> instance = new AtomicReference<>();
    private volatile Thread runner;
    private volatile boolean stopped = false;
    private volatile long currentMillis = System.currentTimeMillis();
    private volatile long currentNanos = System.nanoTime();

    @Override
    public long currentTimeMillis() {
        return currentMillis;
    }

    @Override
    public long nanoTime() {
        return currentNanos;
    }

    private void start() {
        runner = new Thread(new TTask());
        runner.start();
    }

    public void stop() {
        stopped = true;
        runner.interrupt();
    }

    public static TickingClock getInstance() {
        if (instance.get() == null) {
            TickingClock newClock = new TickingClock();
            if (instance.compareAndSet(null, newClock)) {
                newClock.start();
            }
        }
        return instance.get();
    }

    private class TTask implements Runnable {

        @Override
        public void run() {
            long waitTime = 50000;

            while (!stopped) {
                LockSupport.parkNanos(waitTime);
                currentMillis = System.currentTimeMillis();
                long newTime = System.nanoTime();
                gram.recordValue(newTime - currentNanos);
                currentNanos = newTime;

            }
        }
    }
}
