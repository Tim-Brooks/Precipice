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

package net.uncontended.precipice.pattern;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Shotgun implements PatternStrategy {

    private final int acquireCount;
    private final int serviceCount;
    private final Integer[] serviceIndices;

    public Shotgun(int serviceCount, int acquireCount) {
        this.serviceCount = serviceCount;
        this.acquireCount = acquireCount;
        this.serviceIndices = new Integer[serviceCount];
        for (int i = 0; i < serviceCount; ++i) {
            serviceIndices[i] = i;
        }
    }

    @Override
    public Iterable<Integer> nextIndices() {
        SingleReaderArrayIterable iterable = new SingleReaderArrayIterable(serviceCount);
        Integer[] orderToTry = iterable.getIndices();

        System.arraycopy(serviceIndices, 0, orderToTry, 0, serviceCount);
        shuffle(orderToTry);

        return iterable;
    }

    @Override
    public int acquireCount() {
        return acquireCount;
    }

    private static void shuffle(Integer[] orderToTry) {
        int index;
        Random random = ThreadLocalRandom.current();
        for (int i = orderToTry.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
                orderToTry[index] ^= orderToTry[i];
                orderToTry[i] ^= orderToTry[index];
                orderToTry[index] ^= orderToTry[i];
            }
        }
    }
}
