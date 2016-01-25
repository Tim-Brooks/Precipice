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

import java.util.List;

public class ExperimentalLoadBalancer<T extends Enum<T> & Result, V extends Controllable<T>> implements Controllable<T> {

    private final List<V> children;
    private final Controller<T> controller;
    private final LoadBalancerStrategy strategy;

    public ExperimentalLoadBalancer(Controller<T> controller, List<V> children, LoadBalancerStrategy strategy) {
        this.controller = controller;
        this.children = children;
        this.strategy = strategy;
    }

    public V next() {
        int firstServiceToTry = strategy.nextExecutorIndex();

        int j = 0;
        int serviceCount = children.size();
        while (j < serviceCount) {
            int serviceIndex = (firstServiceToTry + j) % serviceCount;
            V controllable = children.get(serviceIndex);
            Controller<T> controller = controllable.controller();
            Rejected rejected = controller.acquirePermitOrGetRejectedReason();
            if (rejected == null) {
                return controllable;
            }
        }
        Rejected reason = Rejected.ALL_SERVICES_REJECTED;
        controller.getActionMetrics().incrementRejectionCount(reason);
        throw new RejectedException(reason);
    }

    @Override
    public Controller<T> controller() {
        return controller;
    }
}
