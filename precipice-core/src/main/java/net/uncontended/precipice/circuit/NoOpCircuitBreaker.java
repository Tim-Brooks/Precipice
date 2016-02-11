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

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;

import java.util.concurrent.atomic.AtomicBoolean;

public class NoOpCircuitBreaker<Rejected extends Enum<Rejected>> implements BPCircuitBreakerInterface<Rejected> {
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);

    @Override
    public Rejected acquirePermit(long number, long nanoTime) {
        return null;
    }

    @Override
    public void releasePermit(long number, long nanoTime) {

    }

    @Override
    public void releasePermit(long number, Failable result, long nanoTime) {

    }

    @Override
    public <Result extends Enum<Result> & Failable> void registerGuardRail(GuardRail<Result, Rejected> guardRail) {
    }

    @Override
    public boolean isOpen() {
        return circuitOpen.get();
    }

    @Override
    public BPBreakerConfig<Rejected> getBreakerConfig() {
        return null;
    }

    @Override
    public void setBreakerConfig(BPBreakerConfig<Rejected> breakerConfig) {
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
