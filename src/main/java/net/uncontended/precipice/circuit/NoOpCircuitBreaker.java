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

package net.uncontended.precipice.circuit;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by timbrooks on 11/5/14.
 */
public class NoOpCircuitBreaker implements CircuitBreaker {
    private static final BreakerConfig config = new BreakerConfig(Integer.MAX_VALUE, 0, 0, 0, 0);
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);

    @Override
    public boolean isOpen() {
        return circuitOpen.get();
    }

    @Override
    public boolean allowAction() {
        return !isOpen();
    }

    @Override
    public void informBreakerOfResult(boolean successful) {
    }

    @Override
    public BreakerConfig getBreakerConfig() {
        return config;
    }

    @Override
    public void setBreakerConfig(BreakerConfig breakerConfig) {

    }

    @Override
    public void forceOpen() {
        circuitOpen.set(true);
    }

    @Override
    public void forceClosed() {
        circuitOpen.set(false);
    }
}
