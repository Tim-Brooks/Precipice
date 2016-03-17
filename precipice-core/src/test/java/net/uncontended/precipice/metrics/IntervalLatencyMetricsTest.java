/*
 * Copyright 2015 Timothy Brooks
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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.test_utils.TestResult;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class IntervalLatencyMetricsTest {

    private IntervalLatencyMetrics<TestResult> metrics;

    @Before
    public void setup() {
        metrics = new IntervalLatencyMetrics<>(TestResult.class);
    }

    @Test
    public void latencyIsStoredInHistogram() {
        TestResult[] metricArray = {TestResult.SUCCESS, TestResult.ERROR};

        ThreadLocalRandom current = ThreadLocalRandom.current();
        for (int i = 1; i <= 100000; ++i) {
            int n = current.nextInt(2);
            metrics.recordLatency(metricArray[n], 1L, i);
        }

        // TODO: For side effects
        metrics.intervalSnapshot(TestResult.SUCCESS);
        metrics.intervalSnapshot(TestResult.ERROR);
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
            metrics.recordLatency(TestResult.SUCCESS, 1L, i);
        }
        for (int i = 100001; i <= 200000; ++i) {
            metrics.recordLatency(TestResult.ERROR, 1L, i);
        }

        // TODO: For side effects
        metrics.intervalSnapshot(TestResult.SUCCESS);
        metrics.intervalSnapshot(TestResult.ERROR);

        LatencySnapshot successSnapshot = metrics.latencySnapshot(TestResult.SUCCESS);
        assertEquals(100, successSnapshot.latencyMax / 1000);
        assertEquals(50, successSnapshot.latency50 / 1000);
        assertEquals(90, successSnapshot.latency90 / 1000);
        assertEquals(99, successSnapshot.latency99 / 1000);
        assertEquals(100, successSnapshot.latency999 / 1000);
        assertEquals(100, successSnapshot.latency9999 / 1000);
        assertEquals(100, successSnapshot.latency99999 / 1000);
        assertEquals(50, (long) successSnapshot.latencyMean / 1000);

        LatencySnapshot errorSnapshot = metrics.latencySnapshot(TestResult.ERROR);
        assertEquals(200, errorSnapshot.latencyMax / 1000);
        assertEquals(150, errorSnapshot.latency50 / 1000);
        assertEquals(190, errorSnapshot.latency90 / 1000);
        assertEquals(199, errorSnapshot.latency99 / 1000);
        assertEquals(200, errorSnapshot.latency999 / 1000);
        assertEquals(200, errorSnapshot.latency9999 / 1000);
        assertEquals(200, errorSnapshot.latency99999 / 1000);
        assertEquals(150, (long) errorSnapshot.latencyMean / 1000);

        LatencySnapshot snapshot = metrics.latencySnapshot();
        assertEquals(200, snapshot.latencyMax / 1000);
        assertEquals(100, snapshot.latency50 / 1000);
        assertEquals(180, snapshot.latency90 / 1000);
        assertEquals(198, snapshot.latency99 / 1000);
        assertEquals(200, snapshot.latency999 / 1000);
        assertEquals(200, snapshot.latency9999 / 1000);
        assertEquals(200, snapshot.latency99999 / 1000);
        assertEquals(100, (long) snapshot.latencyMean / 1000);
    }

    @Test
    public void intervalLatencyIsPartitionedByMetric() {
        for (int i = 1; i <= 100000; ++i) {
            metrics.recordLatency(TestResult.SUCCESS, 1L, i);
        }
        for (int i = 100001; i <= 200000; ++i) {
            metrics.recordLatency(TestResult.ERROR, 1L, i);
        }

        LatencySnapshot successSnapshot = metrics.intervalSnapshot(TestResult.SUCCESS);
        assertEquals(100, successSnapshot.latencyMax / 1000);
        assertEquals(50, successSnapshot.latency50 / 1000);
        assertEquals(90, successSnapshot.latency90 / 1000);
        assertEquals(99, successSnapshot.latency99 / 1000);
        assertEquals(100, successSnapshot.latency999 / 1000);
        assertEquals(100, successSnapshot.latency9999 / 1000);
        assertEquals(100, successSnapshot.latency99999 / 1000);
        assertEquals(50, (long) successSnapshot.latencyMean / 1000);

        LatencySnapshot errorSnapshot = metrics.intervalSnapshot(TestResult.ERROR);
        assertEquals(200, errorSnapshot.latencyMax / 1000);
        assertEquals(150, errorSnapshot.latency50 / 1000);
        assertEquals(190, errorSnapshot.latency90 / 1000);
        assertEquals(199, errorSnapshot.latency99 / 1000);
        assertEquals(200, errorSnapshot.latency999 / 1000);
        assertEquals(200, errorSnapshot.latency9999 / 1000);
        assertEquals(200, errorSnapshot.latency99999 / 1000);
        assertEquals(150, (long) errorSnapshot.latencyMean / 1000);
    }

    @Test
    public void latencyIsRecordedInIntervals() {
        TestResult[] metricArray = {TestResult.SUCCESS, TestResult.ERROR};
        for (TestResult m : metricArray) {

            for (int i = 1; i <= 100000; ++i) {
                metrics.recordLatency(m, 1L, i);
            }
            LatencySnapshot successSnapshot = metrics.intervalSnapshot(m);
            for (int i = 100001; i <= 200000; ++i) {
                metrics.recordLatency(m, 1L, i);
            }
            LatencySnapshot successSnapshot2 = metrics.intervalSnapshot(m);

            for (int i = 200001; i <= 300000; ++i) {
                metrics.recordLatency(m, 1L, i);
            }
            LatencySnapshot successSnapshot3 = metrics.intervalSnapshot(m);

            assertEquals(100, successSnapshot.latencyMax / 1000);
            assertEquals(50, successSnapshot.latency50 / 1000);
            assertEquals(90, successSnapshot.latency90 / 1000);
            assertEquals(99, successSnapshot.latency99 / 1000);
            assertEquals(100, successSnapshot.latency999 / 1000);
            assertEquals(100, successSnapshot.latency9999 / 1000);
            assertEquals(100, successSnapshot.latency99999 / 1000);
            assertEquals(50, (long) successSnapshot.latencyMean / 1000);

            assertEquals(200, successSnapshot2.latencyMax / 1000);
            assertEquals(150, successSnapshot2.latency50 / 1000);
            assertEquals(190, successSnapshot2.latency90 / 1000);
            assertEquals(199, successSnapshot2.latency99 / 1000);
            assertEquals(200, successSnapshot2.latency999 / 1000);
            assertEquals(200, successSnapshot2.latency9999 / 1000);
            assertEquals(200, successSnapshot2.latency99999 / 1000);
            assertEquals(150, (long) successSnapshot2.latencyMean / 1000);

            assertEquals(301, successSnapshot3.latencyMax / 1000);
            assertEquals(250, successSnapshot3.latency50 / 1000);
            assertEquals(290, successSnapshot3.latency90 / 1000);
            assertEquals(299, successSnapshot3.latency99 / 1000);
            assertEquals(301, successSnapshot3.latency999 / 1000);
            assertEquals(301, successSnapshot3.latency9999 / 1000);
            assertEquals(301, successSnapshot3.latency99999 / 1000);
            assertEquals(250, (long) successSnapshot3.latencyMean / 1000);
        }
    }
}
