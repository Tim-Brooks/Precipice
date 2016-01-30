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
import net.uncontended.precipice.concurrent.PrecipicePromise;

import java.util.List;

public class NewShotgun<T extends Enum<T> & Result, C extends Controllable<T>> implements Controllable<T> {

    private final Controller<T> controller;
    private final List<C> pool;
    private final ShotgunStrategy strategy;

    public NewShotgun(Controller<T> controller, List<C> pool, ShotgunStrategy strategy) {
        this.controller = controller;
        this.pool = pool;
        this.strategy = strategy;
    }

    @Override
    public Controller<T> controller() {
        return controller;
    }

    public <R> PatternEntry<C, PrecipicePromise<T, R>> promisePair() {
        return promisePair(null);
    }

    public <R> PatternEntry<C, PrecipicePromise<T, R>> promisePair(PrecipicePromise<T, R> externalPromise) {
        long nanoTime = acquirePermit();
        C[] children = controllableArray(nanoTime);
        return prepIterator(nanoTime, controller.getPromise(nanoTime, externalPromise), children);
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

    private <R> PatternEntry<C, PrecipicePromise<T, R>> prepIterator(long nanoTime, PrecipicePromise<T, R> promise, C[] children) {
        int size = strategy.getSubmissionCount();
        NewEntry<C, PrecipicePromise<T, R>>[] objects = (NewEntry<C, PrecipicePromise<T, R>>[]) new Object[size];
        for (int i = 0; i < size; ++i) {
            objects[i] = new NewEntry<>();
        }

        PatternEntry<C, PrecipicePromise<T, R>> entry = new PatternEntry<>(objects);
        entry.setPatternCompletable(promise);
        PatternEntry.PairIterator<C, PrecipicePromise<T, R>> iterator = entry.iterator;

        int i = 0;
        for (C child : children) {
            if (child != null) {
                NewEntry<C, PrecipicePromise<T, R>> smallEntry = iterator.get(i);
                smallEntry.controllable = child;
                smallEntry.completable = child.controller().getPromise(nanoTime, promise);
            } else {
                break;
            }
            ++i;
        }

        return entry;
    }
}
