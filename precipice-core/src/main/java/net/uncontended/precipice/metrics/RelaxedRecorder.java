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

package net.uncontended.precipice.metrics;

public abstract class RelaxedRecorder<T extends Enum<T>, V> extends AbstractMetrics<T> implements Recorder<V> {
    protected volatile Holder<V> activeHolder = new Holder<>();
    protected volatile Holder<V> inactiveHolder = new Holder<>();

    public RelaxedRecorder(Class<T> clazz, V initialValue, long nanoTime) {
        super(clazz);
        activeHolder.metrics = initialValue;
        activeHolder.endNanos = nanoTime;
    }

    public V active() {
        return activeHolder.metrics;
    }

    public synchronized V flip(long nanoTime, V newCounter) {
        Holder<V> old = this.activeHolder;
        Holder<V> newHolder = this.inactiveHolder;
        newHolder.metrics = newCounter;
        newHolder.startNanos = nanoTime;
        old.endNanos = nanoTime;
        activeHolder = newHolder;
        inactiveHolder = old;
        return old.metrics;
    }

    protected static class Holder<V> {
        protected long startNanos;
        protected long endNanos;
        protected V metrics;
    }
}
