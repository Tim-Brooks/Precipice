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

package net.uncontended.precipice.core.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Channel<T> {

    private final Promise<T>[] promises;
    private int tail = 0;
    private AtomicInteger head = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    public Channel(int size) {
        promises = (Promise<T>[]) new Promise[size];
    }

    public Promise<T> select() {
        return select(-1, null);
    }

    public Promise<T> select(long duration, TimeUnit unit) {
        for (; ; ) {
            Promise<T> promise = promises[tail];
            if (promise != null) {
                ++tail;
                return promise;
            } else {
                return null;
            }
        }
    }

    public void put(Promise<T> promise) {
        int currentHead = head.getAndIncrement();
        promises[currentHead] = promise;
    }
}
