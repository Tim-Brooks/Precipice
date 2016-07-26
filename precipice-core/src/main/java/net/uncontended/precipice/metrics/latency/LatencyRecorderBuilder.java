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

import net.uncontended.precipice.metrics.tools.RecorderBuilder;
import net.uncontended.precipice.metrics.tools.RelaxedRecorder;

public class LatencyRecorderBuilder<T extends Enum<T>> extends RecorderBuilder<PartitionedLatency<T>, LatencyRecorder<T>> {

    @Override
    public LatencyRecorder<T> build() {
        if (active == null) {
            throw new IllegalArgumentException("Initial active object cannot be null");
        }

        if (inactive == null) {
            throw new IllegalArgumentException("Initial inactive object cannot be null");
        }

        if (recorder == null) {
            recorder = new RelaxedRecorder<>();
        }
        return new LatencyRecorder<T>(active, inactive, recorder);
    }
}
