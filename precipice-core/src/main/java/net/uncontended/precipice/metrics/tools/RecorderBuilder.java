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

package net.uncontended.precipice.metrics.tools;

public abstract class RecorderBuilder<T, S> {

    protected Recorder<T> recorder;

    protected T active;
    protected T inactive;

    public RecorderBuilder<T, S> initialActive(T active) {
        this.active = active;
        return this;
    }

    public RecorderBuilder<T, S> initialInactive(T inactive) {
        this.inactive = inactive;
        return this;
    }

    public RecorderBuilder<T, S> withRecorder(Recorder<T> recorder) {
        this.recorder = recorder;
        return this;
    }

    public abstract S build();

}
