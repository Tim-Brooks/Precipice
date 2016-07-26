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

package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.Metrics;
import net.uncontended.precipice.metrics.Resettable;
import org.HdrHistogram.Histogram;

public interface PartitionedHistogram<T extends Enum<T>> extends Metrics<T>, Resettable {

    void record(T metric, long number, long nanoLatency);

    Histogram getHistogram(T metric);

    @Override
    void reset();
}
