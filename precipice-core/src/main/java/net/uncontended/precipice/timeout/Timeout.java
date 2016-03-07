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

package net.uncontended.precipice.timeout;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

class Timeout implements Delayed {

    private final TimeoutTask task;
    public final long nanosAbsoluteTimeout;
    public final long millisRelativeTimeout;

    public Timeout(TimeoutTask task, long millisRelativeTimeout, long nanosAbsoluteStart) {
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
        if (o instanceof Timeout) {
            return Long.compare(nanosAbsoluteTimeout, ((Timeout) o).nanosAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
    }

    public void setTimedOut() {
        task.setTimedOut();
    }
}
