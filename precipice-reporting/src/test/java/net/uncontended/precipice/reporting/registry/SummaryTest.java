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
import net.uncontended.precipice.metrics.RollingCountMetrics;
import net.uncontended.precipice.result.SimpleResult;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SummaryTest {

    private RollingCountMetrics<SimpleResult> resultMetrics;
    private RollingCountMetrics<Rejected> rejectedMetrics;
    private Summary<SimpleResult, Rejected> summary;
    private long startTime;

    @Before
    public void setUp() {
        SummaryProperties properties = new SummaryProperties();
        properties.bufferSize = 4;

        resultMetrics = new RollingCountMetrics<>(SimpleResult.class, 8, 500, TimeUnit.MILLISECONDS, new Thing());
        rejectedMetrics = new RollingCountMetrics<>(Rejected.class, 8, 500, TimeUnit.MILLISECONDS, new Thing());
        startTime = System.nanoTime();

        GuardRailBuilder<SimpleResult, Rejected> builder = new GuardRailBuilder<>();
        builder.name("Test");
        builder.resultMetrics(resultMetrics);
        builder.rejectedMetrics(rejectedMetrics);
        summary = new Summary<>(properties, builder.build());
    }

    @Test
    public void testRefresh() {

        for (int i = 0; i < 10; ++i) {
            resultMetrics.add(SimpleResult.SUCCESS, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
            resultMetrics.add(SimpleResult.ERROR, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
        }

        summary.refresh(startTime + TimeUnit.SECONDS.toNanos(1));

        for (int i = 10; i < 15; ++i) {
            resultMetrics.add(SimpleResult.SUCCESS, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
            resultMetrics.add(SimpleResult.ERROR, 1, startTime + (i * TimeUnit.MILLISECONDS.toNanos(100)));
        }

        summary.refresh(startTime + TimeUnit.SECONDS.toNanos(2));
    }

    private static class Thing implements Clock {

        private long currentTime = 0;

        @Override
        public long currentTimeMillis() {
            long currentTime = this.currentTime;
            this.currentTime += 1000;
            return currentTime;
        }

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    }

    private enum Rejected {
        REJECTED_1,
        REJECTED_2;
    }
}
