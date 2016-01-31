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

public class NewShotgun<T extends Enum<T> & Result, C extends Controllable<T>> implements Controllable<T> {

    private final Controller<T> controller;
    private final List<C> pool;
    private final ShotgunStrategy strategy;
    private ThreadLocal<Iterable<Controllable<T>>> local = new ThreadLocal<>();

    public NewShotgun(Controller<T> controller, List<C> pool, ShotgunStrategy strategy) {
        this.controller = controller;
        this.pool = pool;
        this.strategy = strategy;
    }

    @Override
    public Controller<T> controller() {
        return controller;
    }

    public Iterable<C> newer() {
        final long nanoTime = acquirePermit();
        final C[] children = controllableArray(nanoTime);


        return new ControllableIterable<>(nanoTime, children);
    }


    private C[] controllableArray(long nanoTime) {
        int[] servicesToTry = strategy.executorIndices();
        C[] controllableArray = (C[]) new Object[servicesToTry.length];
        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            C controllable = pool.get(serviceIndex);
            Controller<T> controller = controllable.controller();
            Rejected rejected = controller.acquirePermitOrGetRejectedReason();
            if (rejected == null) {
                controllableArray[submittedCount] = controllable;
                ++submittedCount;
            } else {
                controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            }
            if (submittedCount == strategy.getSubmissionCount()) {
                break;
            }
        }
        if (submittedCount == 0) {
            controller.getSemaphore().releasePermit(1);
            controller.getActionMetrics().incrementRejectionCount(Rejected.ALL_SERVICES_REJECTED);
            throw new RejectedException(Rejected.ALL_SERVICES_REJECTED);
        }

        return controllableArray;
    }

    private long acquirePermit() {
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        long nanoTime = System.nanoTime();
        if (rejected != null) {
            controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            throw new RejectedException(rejected);
        }
        return nanoTime;
    }

    private static class ControllableIterable<C> implements Iterable<C>, Iterator<C> {

        private final long nanoTime;
        private final C[] children;
        long nanoTime0;
        private int index = 0;
        private int count = 0;

        public ControllableIterable(long nanoTime, C[] children) {
            this.nanoTime = nanoTime;
            this.children = children;
            nanoTime0 = nanoTime;
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

        public void incrementCount() {
            ++count;
        }

        public void reset() {
            index = 0;
            count = 0;
        }
    }

}
