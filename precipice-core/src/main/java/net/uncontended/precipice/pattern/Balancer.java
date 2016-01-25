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

import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.RejectedException;
import net.uncontended.precipice.Result;
import net.uncontended.precipice.concurrent.PrecipicePromise;

import java.util.List;

public class Balancer<T extends Enum<T> & Result> {

    private final Controller<T> controller;
    private final List<Controller<T>> pool;

    public Balancer(Controller<T> controller, List<Controller<T>> pool) {
        this.controller = controller;
        this.pool = pool;
    }

    public <R> PatternPair<T, R> next() {
        PrecipicePromise<T, R> promise = controller.acquirePermitAndGetPromise();
        Controller<T> controller = pool.get(0);
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        if (rejected != null) {
            throw new RejectedException(rejected);
        }
        // TODO: Double:
        long startTime = System.nanoTime();
        return new PatternPair<>(controller, startTime, promise);
    }

    private static class PatternPair<T extends Enum<T> & Result, R> {
        private final Controller<T> controller;
        private final long startTime;
        private final PrecipicePromise<T, R> parent;

        private PatternPair(Controller<T> controller, long startTime, PrecipicePromise<T, R> parent) {
            this.controller = controller;
            this.startTime = startTime;
            this.parent = parent;
        }

        public PrecipicePromise<T, R> promise() {
            return controller.getPromise(startTime, parent);
        }
    }
}
