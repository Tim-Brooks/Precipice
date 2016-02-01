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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.*;

import java.util.List;

public class Pattern<T extends Enum<T> & Result, C extends Controllable<T>> {

    private final List<C> pool;
    private final Strategy strategy;
    private ThreadLocal<ControllableIterable<C>> local = new ThreadLocal<>();

    public Pattern(List<C> pool, Strategy strategy) {
        this.pool = pool;
        this.strategy = strategy;
    }

    public ControllableIterable<C> getControllables(long nanoTime) {
        ControllableIterable<C> controllableIterable = getControllableIterable();
        addControllables(nanoTime, controllableIterable);

        return controllableIterable;
    }

    private void addControllables(long nanoTime, ControllableIterable<C> controllableIterable) {
        int[] servicesToTry = strategy.nextIndices();
        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            C controllable = pool.get(serviceIndex);
            Controller<T> controller = controllable.controller();
            Rejected rejected = controller.acquirePermitOrGetRejectedReason();
            if (rejected == null) {
                controllableIterable.add(controllable);
                ++submittedCount;
            } else {
                controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            }
            if (submittedCount == strategy.submissionCount()) {
                break;
            }
        }
    }

    private ControllableIterable<C> getControllableIterable() {
        ControllableIterable<C> controllableIterable = local.get();

        if (controllableIterable == null) {
            C[] children = (C[]) new Object[strategy.submissionCount()];
            controllableIterable = new ControllableIterable<>(children);
            local.set(controllableIterable);
        }
        controllableIterable.reset();

        return controllableIterable;

    }

}
