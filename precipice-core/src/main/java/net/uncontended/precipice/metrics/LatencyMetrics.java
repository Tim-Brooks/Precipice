/*
 * Copyright 2015 Timothy Brooks
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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.Failable;

public interface LatencyMetrics<T extends Enum<T> & Failable> {

    LatencySnapshot DEFAULT_SNAPSHOT = new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, -1, -1);

    void recordLatency(T result, long nanoLatency);

    void recordLatency(T result, long nanoLatency, long nanoTime);

    LatencySnapshot latencySnapshot();

    LatencySnapshot latencySnapshot(T result);
}
