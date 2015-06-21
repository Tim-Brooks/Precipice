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

package net.uncontended.precipice;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ShotgunStrategy {

    public final int submissionCount;
    private final int serviceCount;
    private final int[] serviceIndices;

    public ShotgunStrategy(int serviceCount, int submissionCount) {
        this.serviceCount = serviceCount;
        this.submissionCount = submissionCount;
        this.serviceIndices = new int[serviceCount];
        for (int i = 0; i < serviceCount; ++i) {
            serviceIndices[i] = i;
        }
    }

    public int[] executorIndices() {
        int[] orderToTry = new int[serviceCount];

        System.arraycopy(serviceIndices, 0, orderToTry, 0, serviceCount);
        shuffle(orderToTry);

        return orderToTry;
    }

    private void shuffle(int[] orderToTry) {
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
