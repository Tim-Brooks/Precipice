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

package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public abstract class RecorderBuilder<T, S> {

    protected FlipControl<T> flipControl;

    protected T active;
    protected T inactive;
    protected Allocator<T> allocator;
    protected Clock clock = SystemTime.getInstance();

    public RecorderBuilder<T, S> initialActive(T active) {
        this.active = active;
        return this;
    }

    public RecorderBuilder<T, S> initialInactive(T inactive) {
        this.inactive = inactive;
        return this;
    }

    public RecorderBuilder<T, S> withAllocator(Allocator<T> allocator) {
        this.allocator = allocator;
        return this;
    }

    public RecorderBuilder<T, S> withRecorder(FlipControl<T> flipControl) {
        this.flipControl = flipControl;
        return this;
    }

    public RecorderBuilder<T, S> withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public abstract S build();

}
