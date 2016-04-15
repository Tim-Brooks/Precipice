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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.metrics.Recorder;
import net.uncontended.precipice.metrics.RelaxedRecorder;

public class MetricRecorder<T> implements NewMetrics<T> {

    private final T total;
    private final Recorder<T> recorder;

    public MetricRecorder(T total) {
        this(null, total, new RelaxedRecorder<T>());
    }

    public MetricRecorder(T initialActive, T total) {
        this(initialActive, total, new RelaxedRecorder<T>());
    }


    public MetricRecorder(T initialActive, T total, Recorder<T> recorder) {
        this.recorder = recorder;
        this.recorder.flip(initialActive);
        this.total = total;
    }

    @Override
    public T current() {
        return this.recorder.active();
    }

    @Override
    public T current(long nanoTime) {
        return this.recorder.active();
    }

    @Override
    public T total() {
        return total;
    }

    public synchronized T flip(T newValue) {
        return recorder.flip(newValue);
    }
}
