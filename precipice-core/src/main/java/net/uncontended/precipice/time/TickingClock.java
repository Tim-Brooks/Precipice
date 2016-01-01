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

import java.util.concurrent.atomic.AtomicReference;

public class TickingClock implements Clock {

    private static AtomicReference<TickingClock> instance = new AtomicReference<>();
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
}
