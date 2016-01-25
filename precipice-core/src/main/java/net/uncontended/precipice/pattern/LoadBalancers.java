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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.threadpool.ThreadPoolService;

import java.util.Map;

public final class LoadBalancers {

    private LoadBalancers() {
    }

    public static <C> AsyncPattern<C> asyncRoundRobin(String name, Map<? extends ThreadPoolService, C> serviceToContext,
                                                      PatternControllerProperties<Status> properties) {
        PatternController<Status> controller = new PatternController<>(name, properties);
        return new AsyncLoadBalancer<>(serviceToContext, new RoundRobinStrategy(serviceToContext.size()), controller);
    }
}
