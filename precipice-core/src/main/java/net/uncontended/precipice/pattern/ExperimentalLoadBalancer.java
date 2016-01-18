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

import net.uncontended.precipice.NewController;
import net.uncontended.precipice.Status;

public class ExperimentalLoadBalancer {

    private final PatternController<Status> controller;
    private final LoadBalancerStrategy strategy = new RoundRobinStrategy(10);

    public ExperimentalLoadBalancer(PatternController<Status> controller) {
        this.controller = controller;
    }

    public void thing() {
        NewController<Status>[] childControllers = controller.getChildControllers();




    }
}
