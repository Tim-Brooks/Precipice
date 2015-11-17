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

package net.uncontended.precipice.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DefaultLatencyMetricsTest {

    private DefaultLatencyMetrics metrics;

    @Before
    public void setup() {
        metrics = new DefaultLatencyMetrics();
    }

    @Test
    public void latencyIsStoredInHistogram() {
        Metric[] metricArray = new Metric[3];
        metricArray[0] = Metric.SUCCESS;
        metricArray[1] = Metric.ERROR;
        metricArray[2] = Metric.TIMEOUT;

        ThreadLocalRandom current = ThreadLocalRandom.current();
        for (int i = 1; i <= 100000; ++i) {
            int n = current.nextInt(3);
            metrics.recordLatency(metricArray[n], i);
        }

        LatencySnapshot snapshot = metrics.latencySnapshot();

        assertEquals(100, snapshot.latencyMax / 1000);
        assertEquals(50, snapshot.latency50 / 1000);
        assertEquals(90, snapshot.latency90 / 1000);
        assertEquals(99, snapshot.latency99 / 1000);
        assertEquals(100, snapshot.latency999 / 1000);
        assertEquals(100, snapshot.latency9999 / 1000);
        assertEquals(100, snapshot.latency99999 / 1000);
        assertEquals(50, (long) snapshot.latencyMean / 1000);
    }

    @Test
    public void latencyIsPartitionedByMetric() {
        for (int i = 1; i <= 100000; ++i) {
            metrics.recordLatency(Metric.SUCCESS, i);
        }
        for (int i = 100001; i <= 200000; ++i) {
            metrics.recordLatency(Metric.ERROR, i);
        }
        for (int i = 200001; i <= 300000; ++i) {
            metrics.recordLatency(Metric.TIMEOUT, i);
        }

        LatencySnapshot successSnapshot = metrics.latencySnapshot(Metric.SUCCESS);
        assertEquals(100, successSnapshot.latencyMax / 1000);
        assertEquals(50, successSnapshot.latency50 / 1000);
        assertEquals(90, successSnapshot.latency90 / 1000);
        assertEquals(99, successSnapshot.latency99 / 1000);
        assertEquals(100, successSnapshot.latency999 / 1000);
        assertEquals(100, successSnapshot.latency9999 / 1000);
        assertEquals(100, successSnapshot.latency99999 / 1000);
        assertEquals(50, (long) successSnapshot.latencyMean / 1000);

        LatencySnapshot errorSnapshot = metrics.latencySnapshot(Metric.ERROR);
        assertEquals(200, errorSnapshot.latencyMax / 1000);
        assertEquals(150, errorSnapshot.latency50 / 1000);
        assertEquals(190, errorSnapshot.latency90 / 1000);
        assertEquals(199, errorSnapshot.latency99 / 1000);
        assertEquals(200, errorSnapshot.latency999 / 1000);
        assertEquals(200, errorSnapshot.latency9999 / 1000);
        assertEquals(200, errorSnapshot.latency99999 / 1000);
        assertEquals(150, (long) errorSnapshot.latencyMean / 1000);

        LatencySnapshot timeoutSnapshot = metrics.latencySnapshot(Metric.TIMEOUT);
        assertEquals(301, timeoutSnapshot.latencyMax / 1000);
        assertEquals(250, timeoutSnapshot.latency50 / 1000);
        assertEquals(290, timeoutSnapshot.latency90 / 1000);
        assertEquals(299, timeoutSnapshot.latency99 / 1000);
        assertEquals(301, timeoutSnapshot.latency999 / 1000);
        assertEquals(301, timeoutSnapshot.latency9999 / 1000);
        assertEquals(301, timeoutSnapshot.latency99999 / 1000);
        assertEquals(250, (long) timeoutSnapshot.latencyMean / 1000);

        LatencySnapshot snapshot = metrics.latencySnapshot();
        assertEquals(301, snapshot.latencyMax / 1000);
        assertEquals(150, snapshot.latency50 / 1000);
        assertEquals(270, snapshot.latency90 / 1000);
        assertEquals(299, snapshot.latency99 / 1000);
        assertEquals(301, snapshot.latency999 / 1000);
        assertEquals(301, snapshot.latency9999 / 1000);
        assertEquals(301, snapshot.latency99999 / 1000);
        assertEquals(150, (long) snapshot.latencyMean / 1000);
    }

    @Test
    public void testThing() {
        long approximateStart = System.nanoTime();
        long twoMinutes = TimeUnit.MINUTES.toNanos(2);

        metrics.recordLatency(Metric.SUCCESS, 5L, approximateStart + twoMinutes);
        metrics.recordLatency(Metric.SUCCESS, 5L, approximateStart + (twoMinutes * 2));
        metrics.recordLatency(Metric.SUCCESS, 5L, approximateStart + (twoMinutes * 3));
        metrics.recordLatency(Metric.SUCCESS, 5L, approximateStart + (twoMinutes * 4));
        metrics.recordLatency(Metric.SUCCESS, 10L, approximateStart + (twoMinutes * 5));

        for (LatencySnapshot s : metrics.getSnapshotsForPeriod(Metric.SUCCESS, 1, TimeUnit.HOURS, approximateStart + (twoMinutes * 5))) {
            System.out.println(s);
        }


    }

    // TODO: Explore using status opposed to metric
}
