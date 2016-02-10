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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Pattern<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, C extends Precipice<Result, Rejected>> {

    private final List<C> pool;

    private final PatternStrategy strategy;
    private ThreadLocal<SingleReaderSequence<C>> local = new ThreadLocal<>();

    public Pattern(Collection<C> controllables, PatternStrategy strategy) {
        if (controllables.size() == 0) {
            throw new IllegalArgumentException("Cannot create Pattern with 0 Controllables.");
        } else if (strategy.submissionCount() > controllables.size()) {
            throw new IllegalArgumentException("Submission count cannot be greater than the number of controllables " +
                    "provided.");
        }


        List<C> pool = new ArrayList<>(controllables.size());
        pool.addAll(controllables);
        this.pool = pool;
        this.strategy = strategy;
    }

    public Sequence<C> getControllables(long permits, long nanoTime) {
        SingleReaderSequence<C> controllables = getControllableSequence();
        addControllables(permits, nanoTime, controllables);

        return controllables;
    }

    public List<C> getAllControllables() {
        return pool;
    }

    private void addControllables(long permits, long nanoTime, SingleReaderSequence<C> controllables) {
        int[] servicesToTry = strategy.nextIndices();
        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            C controllable = pool.get(serviceIndex);
            GuardRail<Result, Rejected> guardRail = controllable.guardRail();
            Rejected rejected = guardRail.acquirePermitOrGetRejectedReason(permits, nanoTime);
            if (rejected == null) {
                controllables.add(controllable);
                ++submittedCount;
            } else {
                guardRail.getRejectedMetrics().incrementMetricCount(rejected, nanoTime);
            }
            if (submittedCount == strategy.submissionCount()) {
                break;
            }
        }
    }

    private SingleReaderSequence<C> getControllableSequence() {
        SingleReaderSequence<C> controllables = local.get();

        if (controllables == null) {
            controllables = new SingleReaderSequence<>(strategy.submissionCount());
            local.set(controllables);
        }
        controllables.reset();

        return controllables;

    }

}
