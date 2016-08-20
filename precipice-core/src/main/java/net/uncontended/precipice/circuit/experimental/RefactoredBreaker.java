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

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.CircuitBreakerConfig;

public class RefactoredBreaker<Rejected extends Enum<Rejected>> extends AbstractBreaker implements CircuitBreaker<Rejected> {

    private final Rejected reason;
    private final Rejected forcedReason;
    private final long backOffTimeNanos;
    private final HealthChecker checker;

    private volatile long lastTestedNanoTime;

    public RefactoredBreaker(Rejected reason, Rejected forcedReason, HealthChecker checker, long backOffTimeNanos) {
        this.reason = reason;
        this.forcedReason = forcedReason;
        this.checker = checker;
        this.backOffTimeNanos = backOffTimeNanos;
    }

    @Override
    public CircuitBreakerConfig<Rejected> getBreakerConfig() {
        return null;
    }

    @Override
    public void setBreakerConfig(CircuitBreakerConfig<Rejected> breakerConfig) {

    }

    @Override
    public Rejected acquirePermit(long number, long nanoTime) {
        int state = this.state.get();
        if (state == OPEN) {
            // This potentially allows a couple of tests through. Should think about this decision
            if (nanoTime - (backOffTimeNanos + lastTestedNanoTime) < 0) {
                return reason;
            }
            lastTestedNanoTime = nanoTime;
        }
        return state != FORCED_OPEN ? null : forcedReason;
    }

    @Override
    public void releasePermit(long number, long nanoTime) {

    }

    @Override
    public void releasePermit(long number, Failable result, long nanoTime) {
        if (result.isSuccess()) {
            if (state.get() == OPEN) {
                // Explore whether this can get stuck in a loop with open and closing
                state.compareAndSet(OPEN, CLOSED);
            }
        } else {
            if (state.get() == CLOSED) {
                if (!checker.isHealthy(nanoTime)) {
                    lastTestedNanoTime = nanoTime;
                    state.compareAndSet(CLOSED, OPEN);
                }
            }
        }
    }

    @Override
    public <Result extends Enum<Result> & Failable> void registerGuardRail(GuardRail<Result, Rejected> guardRail) {
    }
}
