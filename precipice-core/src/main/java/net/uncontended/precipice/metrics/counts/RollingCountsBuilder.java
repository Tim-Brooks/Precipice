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

package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.tools.RollingBuilder;
import net.uncontended.precipice.metrics.tools.RollingMetrics;

public class RollingCountsBuilder<T extends Enum<T>> extends RollingBuilder<PartitionedCount<T>, RollingCounts<T>> {

    private final Class<T> clazz;

    public RollingCountsBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public RollingCounts<T> build() {
        if (allocator == null) {
            allocator = Counters.longAdder(clazz);
        }

        RollingMetrics<PartitionedCount<T>> rollingMetrics = buildRollingMetrics();
        return new RollingCounts<T>(rollingMetrics);
    }
}
