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

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class RoundRobinStrategyTest {

    @Test
    public void wrappingWorks() {
        int start = (Integer.MAX_VALUE / 2) - 1;
        RoundRobinStrategy strategy = new RoundRobinStrategy(3, new AtomicInteger(start));

        assertEquals(start % 3, strategy.nextExecutorIndex());
        assertEquals((start + 1) % 3, strategy.nextExecutorIndex());
        assertEquals(0, strategy.nextExecutorIndex());
        assertEquals(1, strategy.nextExecutorIndex());
        assertEquals(2, strategy.nextExecutorIndex());
    }
}
