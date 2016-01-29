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
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.concurrent.PrecipicePromise;

import java.util.List;

public class Balancer<T extends Enum<T> & Result, C extends Controllable<T>> implements Controllable<T> {

    private final Controller<T> controller;
    private final List<C> pool;
    private final LoadBalancerStrategy strategy;

    public Balancer(Controller<T> controller, List<C> pool, LoadBalancerStrategy strategy) {
        this.controller = controller;
        this.pool = pool;
        this.strategy = strategy;
    }

    @Override
    public Controller<T> controller() {
        return controller;
    }

    public <R> PatternPair<C, PrecipicePromise<T, R>> promisePair() {
        return promisePair(null);
    }

    public <R> PatternPair<C, PrecipicePromise<T, R>> promisePair(PrecipicePromise<T, R> externalPromise) {
        acquirePermit();
        long nanoTime = System.nanoTime();
        C child = nextControllable(nanoTime);
        PrecipicePromise<T, R> promise = controller.getPromise(nanoTime, externalPromise);
        return new PatternPair<>(child, child.controller().getPromise(nanoTime, promise));
    }

    public <R> PatternPair<C, Completable<T, R>> completablePair() {
        return completablePair(null);
    }

    public <R> PatternPair<C, Completable<T, R>> completablePair(Completable<T, R> externalCompletable) {
        acquirePermit();
        long nanoTime = System.nanoTime();
        C child = nextControllable(nanoTime);
        Completable<T, R> completable = controller.getCompletableContext(nanoTime, externalCompletable);
        return new PatternPair<>(child, child.controller().getCompletableContext(nanoTime, completable));
    }

    private void acquirePermit() {
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        if (rejected != null) {
            long nanoTime = System.nanoTime();
            controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            throw new RejectedException(rejected);
        }
    }

    private C nextControllable(long nanoTime) {
        int firstServiceToTry = strategy.nextIndex();

        int serviceCount = pool.size();
        for (int j = 0; j < serviceCount; ++j) {
            int serviceIndex = (firstServiceToTry + j) % serviceCount;
            C controllable = pool.get(serviceIndex);
            Controller<T> controller = controllable.controller();
            Rejected rejected = controller.acquirePermitOrGetRejectedReason();
            if (rejected != null) {
                controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            } else {
                controller.getActionMetrics().incrementRejectionCount(Rejected.ALL_SERVICES_REJECTED, nanoTime);
                controller.getSemaphore().releasePermit(1);
                return controllable;
            }
        }
        throw new RejectedException(Rejected.ALL_SERVICES_REJECTED);
    }

}
