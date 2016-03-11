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

package net.uncontended.precipice.pattern;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ShotgunTest {

    @Test
    public void indicesAreReturnedUniformly() {
        Shotgun shotgun = new Shotgun(3, 2);

        int firstTotal = 0;
        int secondTotal = 0;
        int thirdTotal = 0;
        for (int i = 0; i < 5000; ++i) {
            Iterator<Integer> indices = shotgun.nextIndices().iterator();
            int first = indices.next();
            int second = indices.next();
            int third = indices.next();
            assertFalse(indices.hasNext());
            assertNotEquals(first, second);
            assertNotEquals(first, third);
            assertNotEquals(second, third);
            firstTotal += first;
            secondTotal += second;
            thirdTotal += third;
        }

        double firstMean = (double) firstTotal / 5000;
        double secondMean = (double) secondTotal / 5000;
        double thirdMean = (double) thirdTotal / 5000;

        String message = "Concerning distribution of indices returned";
        assertTrue(message, 0.15 > Math.abs(firstMean - secondMean));
        assertTrue(message, 0.15 > Math.abs(firstMean - thirdMean));
        assertTrue(message, 0.15 > Math.abs(secondMean - thirdMean));
    }
}
