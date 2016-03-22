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
import org.HdrHistogram.Histogram;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntervalLatencyMetricsTest {

    private IntervalLatencyMetrics<TestResult> metrics;

    @Before
    public void setup() {
        metrics = new IntervalLatencyMetrics<>(TestResult.class);
    }

    @Test
    public void latencyIsPartitionedByMetric() {
        for (int i = 1; i <= 100000; ++i) {
            metrics.record(TestResult.SUCCESS, 1L, i);
        }
        for (int i = 100001; i <= 200000; ++i) {
            metrics.record(TestResult.ERROR, 1L, i);
        }

        // For side effects
        metrics.intervalHistogram(TestResult.SUCCESS);
        metrics.intervalHistogram(TestResult.ERROR);

        Histogram successHistogram = metrics.totalHistogram(TestResult.SUCCESS);
        assertEquals(100, successHistogram.getMaxValue() / 1000);
        assertEquals(50, successHistogram.getValueAtPercentile(50.0) / 1000);
        assertEquals(90, successHistogram.getValueAtPercentile(90.0) / 1000);
        assertEquals(99, successHistogram.getValueAtPercentile(99.0) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.9) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.99) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.999) / 1000);
        assertEquals(50.00128128, successHistogram.getMean() / 1000, 0.01);

        Histogram errorHistogram = metrics.totalHistogram(TestResult.ERROR);
        assertEquals(200, errorHistogram.getMaxValue() / 1000);
        assertEquals(150, errorHistogram.getValueAtPercentile(50.0) / 1000);
        assertEquals(190, errorHistogram.getValueAtPercentile(90.0) / 1000);
        assertEquals(199, errorHistogram.getValueAtPercentile(99.0) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.9) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.99) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.999) / 1000);
        assertEquals(150.00128128, errorHistogram.getMean() / 1000, 0.01);
    }

    @Test
    public void intervalLatencyIsPartitionedByMetric() {
        for (int i = 1; i <= 100000; ++i) {
            metrics.record(TestResult.SUCCESS, 1L, i);
        }
        for (int i = 100001; i <= 200000; ++i) {
            metrics.record(TestResult.ERROR, 1L, i);
        }

        Histogram successHistogram = metrics.intervalHistogram(TestResult.SUCCESS);
        assertEquals(100, successHistogram.getMaxValue() / 1000);
        assertEquals(50, successHistogram.getValueAtPercentile(50.0) / 1000);
        assertEquals(90, successHistogram.getValueAtPercentile(90.0) / 1000);
        assertEquals(99, successHistogram.getValueAtPercentile(99.0) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.9) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.99) / 1000);
        assertEquals(100, successHistogram.getValueAtPercentile(99.999) / 1000);
        assertEquals(50.00128128, successHistogram.getMean() / 1000, 0.01);

        Histogram errorHistogram = metrics.intervalHistogram(TestResult.ERROR);
        assertEquals(200, errorHistogram.getMaxValue() / 1000);
        assertEquals(150, errorHistogram.getValueAtPercentile(50.0) / 1000);
        assertEquals(190, errorHistogram.getValueAtPercentile(90.0) / 1000);
        assertEquals(199, errorHistogram.getValueAtPercentile(99.0) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.9) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.99) / 1000);
        assertEquals(200, errorHistogram.getValueAtPercentile(99.999) / 1000);
        assertEquals(150.00128128, errorHistogram.getMean() / 1000, 0.01);
    }

    @Test
    public void latencyIsRecordedInIntervals() {
        TestResult[] metricArray = {TestResult.SUCCESS, TestResult.ERROR};
        for (TestResult m : metricArray) {

            for (int i = 1; i <= 100000; ++i) {
                metrics.record(m, 1L, i);
            }
            Histogram histogram1 = metrics.intervalHistogram(m);

            for (int i = 100001; i <= 200000; ++i) {
                metrics.record(m, 1L, i);
            }
            Histogram histogram2 = metrics.intervalHistogram(m);

            for (int i = 200001; i <= 300000; ++i) {
                metrics.record(m, 1L, i);
            }

            Histogram histogram3 = metrics.intervalHistogram(m);

            assertEquals(100, histogram1.getMaxValue() / 1000);
            assertEquals(50, histogram1.getValueAtPercentile(50.0) / 1000);
            assertEquals(90, histogram1.getValueAtPercentile(90.0) / 1000);
            assertEquals(99, histogram1.getValueAtPercentile(99.0) / 1000);
            assertEquals(100, histogram1.getValueAtPercentile(99.9) / 1000);
            assertEquals(100, histogram1.getValueAtPercentile(99.99) / 1000);
            assertEquals(100, histogram1.getValueAtPercentile(99.999) / 1000);
            assertEquals(50.00128128, histogram1.getMean() / 1000, 0.01);

            assertEquals(200, histogram2.getMaxValue() / 1000);
            assertEquals(150, histogram2.getValueAtPercentile(50.0) / 1000);
            assertEquals(190, histogram2.getValueAtPercentile(90.0) / 1000);
            assertEquals(199, histogram2.getValueAtPercentile(99.0) / 1000);
            assertEquals(200, histogram2.getValueAtPercentile(99.9) / 1000);
            assertEquals(200, histogram2.getValueAtPercentile(99.99) / 1000);
            assertEquals(200, histogram2.getValueAtPercentile(99.999) / 1000);
            assertEquals(150.00128128, histogram2.getMean() / 1000, 0.01);

            assertEquals(301, histogram3.getMaxValue() / 1000);
            assertEquals(250, histogram3.getValueAtPercentile(50.0) / 1000);
            assertEquals(290, histogram3.getValueAtPercentile(90.0) / 1000);
            assertEquals(299, histogram3.getValueAtPercentile(99.0) / 1000);
            assertEquals(301, histogram3.getValueAtPercentile(99.9) / 1000);
            assertEquals(301, histogram3.getValueAtPercentile(99.99) / 1000);
            assertEquals(301, histogram3.getValueAtPercentile(99.999) / 1000);
            assertEquals(250.00128128, histogram3.getMean() / 1000, 0.01);
        }
    }
}
