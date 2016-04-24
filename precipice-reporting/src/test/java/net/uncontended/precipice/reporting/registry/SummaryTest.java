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
 */

package net.uncontended.precipice.reporting.registry;

import net.uncontended.precipice.GuardRailBuilder;
import net.uncontended.precipice.metrics.counts.PartitionedCount;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.result.SimpleResult;
import org.junit.Before;

public class SummaryTest {

    private Rolling<PartitionedCount<SimpleResult>> resultMetrics;
    private Rolling<PartitionedCount<Rejected>> rejectedMetrics;
    private Summary<SimpleResult, Rejected> summary;
    private long startTime;

    @Before
    public void setUp() {
        SummaryProperties properties = new SummaryProperties();
        properties.bufferSize = 4;

//        resultMetrics = new RollingCountMetrics<>(SimpleResult.class, 8, 500, TimeUnit.MILLISECONDS);
//        rejectedMetrics = new RollingCountMetrics<>(Rejected.class, 8, 500, TimeUnit.MILLISECONDS);
        startTime = System.nanoTime();

        GuardRailBuilder<SimpleResult, Rejected> builder = new GuardRailBuilder<>();
        builder.name("Test");
        builder.resultMetrics(resultMetrics);
        builder.rejectedMetrics(rejectedMetrics);
        summary = new Summary<>(properties, builder.build());
    }

//    @Test
//    public void testRefresh() {
//
//        for (int i = 0; i < 12; ++i) {
//            resultMetrics.add(SimpleResult.SUCCESS, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
//            resultMetrics.add(SimpleResult.ERROR, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
//        }
//
//        summary.refresh(10000, startTime + TimeUnit.SECONDS.toNanos(1));
//
//        for (int i = 12; i < 15; ++i) {
//            resultMetrics.add(SimpleResult.SUCCESS, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
//            resultMetrics.add(SimpleResult.ERROR, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
//        }
//
//        summary.refresh(11100, startTime + TimeUnit.SECONDS.toNanos(2));
//
//        summary.refresh(12030, startTime + TimeUnit.SECONDS.toNanos(3));
//
//        summary.refresh(16020, startTime + TimeUnit.SECONDS.toNanos(4));
//
//        for (Slice<SimpleResult, Rejected> slice : summary.getSlices()) {
//            System.out.println("Start: " + slice.startEpoch);
//            System.out.println("End: " + slice.endEpoch);
//            System.out.println(Arrays.toString(slice.resultCounts));
//        }
//    }

    private enum Rejected {
        REJECTED_1,
        REJECTED_2;
    }
}
