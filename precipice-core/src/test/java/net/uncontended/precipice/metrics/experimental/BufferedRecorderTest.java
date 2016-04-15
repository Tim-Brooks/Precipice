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

import net.uncontended.precipice.metrics.LongAdderCounter;
import net.uncontended.precipice.metrics.NoOpCounter;
import net.uncontended.precipice.metrics.PartitionedCount;
import net.uncontended.precipice.result.SimpleResult;
import org.junit.Test;

public class BufferedRecorderTest {

    @Test
    public void testThing() {
        PartitionedCount<SimpleResult> noOpCounter = new NoOpCounter<>(SimpleResult.class);
        MetricRecorder<PartitionedCount<SimpleResult>> metricRecorder = new MetricRecorder<>(noOpCounter);
        BufferedRecorder<PartitionedCount<SimpleResult>> buffered = new BufferedRecorder<>(metricRecorder,
                new NewAllocator<PartitionedCount<SimpleResult>>() {
                    @Override
                    public PartitionedCount<SimpleResult> allocateNew() {
                        return new LongAdderCounter<>(SimpleResult.class);
                    }
                }, 4);

    }
}
