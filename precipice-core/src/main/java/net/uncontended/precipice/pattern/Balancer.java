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

public class Balancer<T extends Enum<T> & Result> implements Controllable<T> {

    private final Controller<T> controller;
    private final List<Controller<T>> pool;
    private final LoadBalancerStrategy strategy;

    public Balancer(Controller<T> controller, List<Controller<T>> pool, LoadBalancerStrategy strategy) {
        this.controller = controller;
        this.pool = pool;
        this.strategy = strategy;
    }

    @Override
    public Controller<T> controller() {
        return controller;
    }

    public <R> PatternPair<T, R> promisePair() {
        acquirePermit();
        long nanoTime = System.nanoTime();
        Controller<T> childController = findNext(nanoTime);
        PrecipicePromise<T, R> promise = controller.getPromise(nanoTime);
        return new PatternPair<>(controller, childController.getPromise(nanoTime, promise));
    }

    public <R> PatternPair2<T, R> completablePair() {
        acquirePermit();
        long nanoTime = System.nanoTime();
        Controller<T> childController = findNext(nanoTime);
        Completable<T, R> completable = controller.getCompletableContext(nanoTime);
        return new PatternPair2<>(controller, childController.getCompletableContext(nanoTime, completable));
    }

    private void acquirePermit() {
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        if (rejected != null) {
            long nanoTime = System.nanoTime();
            controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            throw new RejectedException(rejected);
        }
    }

    private Controller<T> findNext(long nanoTime) {
        int firstServiceToTry = strategy.nextIndex();

        int serviceCount = pool.size();
        for (int j = 0; j < serviceCount; ++j) {
            int serviceIndex = (firstServiceToTry + j) % serviceCount;
            Controller<T> controller = pool.get(serviceIndex);
            Rejected rejected = controller.acquirePermitOrGetRejectedReason();
            if (rejected != null) {
                controller.getActionMetrics().incrementRejectionCount(rejected, nanoTime);
            } else {
                controller.getActionMetrics().incrementRejectionCount(Rejected.ALL_SERVICES_REJECTED, nanoTime);
                controller.getSemaphore().releasePermit(1);
                return controller;
            }
        }
        throw new RejectedException(Rejected.ALL_SERVICES_REJECTED);
    }

    public static class PatternPair<T extends Enum<T> & Result, R> {
        public final Controller<T> controller;
        public final PrecipicePromise<T, R> promise;

        private PatternPair(Controller<T> controller, PrecipicePromise<T, R> promise) {
            this.controller = controller;
            this.promise = promise;
        }
    }

    public static class PatternPair2<T extends Enum<T> & Result, R> {
        public final Controller<T> controller;
        public final Completable<T, R> completable;

        private PatternPair2(Controller<T> controller, Completable<T, R> completable) {
            this.controller = controller;
            this.completable = completable;
        }
    }
}
