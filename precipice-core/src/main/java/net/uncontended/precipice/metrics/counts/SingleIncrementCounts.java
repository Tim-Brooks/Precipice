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

import net.uncontended.precipice.metrics.AbstractMetrics;

public class SingleIncrementCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T> {

    private final WritableCounts<T> wrappedCounts;

    public SingleIncrementCounts(WritableCounts<T> wrappedCounts) {
        super(wrappedCounts.getMetricClazz());
        this.wrappedCounts = wrappedCounts;
    }

    @Override
    public void write(T metric, long number, long nanoTime) {
        wrappedCounts.write(metric, 1, nanoTime);
    }

    public WritableCounts<T> getWrappedCounts() {
        return wrappedCounts;
    }

}
