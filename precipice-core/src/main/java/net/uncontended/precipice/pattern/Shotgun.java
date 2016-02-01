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

import java.util.Iterator;
import java.util.List;

public class Shotgun<T extends Enum<T> & Result, C extends Controllable<T>> {

    private final List<C> pool;
    private final ShotgunStrategy strategy;
    private ThreadLocal<ControllableIterable<C>> local = new ThreadLocal<>();

    public Shotgun(List<C> pool, ShotgunStrategy strategy) {
        this.pool = pool;
        this.strategy = strategy;
    }

    public Iterable<C> getControllables(long nanoTime) {
        ControllableIterable<C> controllableIterable = getControllableIterable();
        controllableIterable.nanoTime = nanoTime;
        addControllables(nanoTime, controllableIterable);

        return controllableIterable;
    }

    private void addControllables(long nanoTime, ControllableIterable<C> controllableIterable) {
        int[] servicesToTry = strategy.executorIndices();
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
            if (submittedCount == strategy.getSubmissionCount()) {
                break;
            }
        }
    }

    private ControllableIterable<C> getControllableIterable() {
        ControllableIterable<C> controllableIterable = local.get();

        if (controllableIterable == null) {
            C[] children = (C[]) new Object[strategy.getSubmissionCount()];
            controllableIterable = new ControllableIterable<>(children);
            local.set(controllableIterable);
        }
        controllableIterable.reset();

        return controllableIterable;

    }

    private static class ControllableIterable<C> implements Iterable<C>, Iterator<C> {

        public long nanoTime;
        private final C[] children;
        private int index = 0;
        private int count = 0;

        public ControllableIterable(C[] children) {
            this.children = children;
        }

        @Override
        public boolean hasNext() {
            return index != count;
        }

        @Override
        public C next() {
            int j = index;
            ++index;
            return children[j];
        }

        @Override
        public Iterator<C> iterator() {
            return this;
        }

        public void add(C child) {
            children[index++] = child;
        }

        public void reset() {
            index = 0;
            count = 0;
        }
    }

}
