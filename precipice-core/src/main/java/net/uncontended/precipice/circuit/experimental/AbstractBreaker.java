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

package net.uncontended.precipice.circuit.experimental;

import java.util.concurrent.atomic.AtomicInteger;

public class AbstractBreaker {
    protected static final int CLOSED = 0;
    protected static final int OPEN = 1;
    protected static final int FORCED_OPEN = 2;

    protected final AtomicInteger state = new AtomicInteger(0);

    public boolean isOpen() {
        return state.get() != CLOSED;
    }

    public void forceOpen() {
        state.set(FORCED_OPEN);
    }

    public void forceClosed() {
        state.set(CLOSED);
    }
}
