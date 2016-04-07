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

public final class Counters {

    private Counters() {
    }

    public static CounterFactory incrementing() {
        return new IncrementingFactory();
    }

    public static CounterFactory adding() {
        return new AddFactory();
    }

    private static class IncrementingFactory implements CounterFactory {

        @Override
        public <T extends Enum<T>> ReadableCountMetrics<T> newCounter(Class<T> clazz) {
            return new IncrementCounter<>(clazz);
        }
    }

    private static class AddFactory implements CounterFactory {
        @Override
        public <T extends Enum<T>> ReadableCountMetrics<T> newCounter(Class<T> clazz) {
            return new AddCounter<>(clazz);
        }
    }
}
