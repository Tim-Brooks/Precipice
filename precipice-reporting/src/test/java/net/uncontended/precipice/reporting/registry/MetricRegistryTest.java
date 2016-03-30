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

package net.uncontended.precipice.reporting.registry;

public class MetricRegistryTest {

//    private static final String serviceName = "Service Name";
//
//    @Mock
//    private Service service;
//    @Mock
//    private CountMetrics countMetrics;
//
//    @Mock
//    private IntervalLatencyMetrics<TimeoutableResult> latencyMetrics;
//    private MetricRegistry registry;
//
//    @Before
//    public void setup() {
//        MockitoAnnotations.initMocks(this);
//
//        when(service.getName()).thenReturn(serviceName);
//        when(service.getActionMetrics()).thenReturn(countMetrics);
//        when(service.getLatencyMetrics()).thenReturn(latencyMetrics);
//        when(countMetrics.getMetricClazz()).thenReturn(TimeoutableResult.class);
//    }
//
//    @Test
//    public void testSummary() throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        Random random = new Random();
//        long pendingN = random.nextInt(100);
//        long capacityN = random.nextInt(1000);
//
//        long successN = random.nextInt(50);
//        long errorN = random.nextInt(50);
//        long timeoutN = random.nextInt(50);
//        long maxConcurrencyN = random.nextInt(50);
//        long circuitOpenN = random.nextInt(50);
//        long allRejectedN = random.nextInt(50);
//
//        AddCounter<TimeoutableResult> counter = new AddCounter<>(TimeoutableResult.class);
//        incrementCounts(counter, TimeoutableResult.SUCCESS, successN);
//        incrementCounts(counter, TimeoutableResult.ERROR, errorN);
//        incrementCounts(counter, TimeoutableResult.TIMEOUT, timeoutN);
//        List<AddCounter<TimeoutableResult>> counters = new ArrayList<>();
//        int bucketCount = random.nextInt(10);
//        for (int i = 0; i < bucketCount; ++i) {
//            AddCounter<TimeoutableResult> mc = new AddCounter<>(TimeoutableResult.class);
//            incrementCounts(mc, TimeoutableResult.SUCCESS, successN);
//            incrementCounts(mc, TimeoutableResult.ERROR, errorN);
//            incrementCounts(mc, TimeoutableResult.TIMEOUT, timeoutN);
//            counters.add(mc);
//        }
//        LatencySnapshot successLatencySnapshot = generateSnapshot(random);
//        LatencySnapshot errorLatencySnapshot = generateSnapshot(random);
//        LatencySnapshot timeoutLatencySnapshot = generateSnapshot(random);
//        LatencySnapshot totalSuccessLatencySnapshot = generateSnapshot(random);
//        LatencySnapshot totalErrorLatencySnapshot = generateSnapshot(random);
//        LatencySnapshot totalTimeoutLatencySnapshot = generateSnapshot(random);
//
//        when(service.pendingCount()).thenReturn(pendingN);
//        when(service.remainingCapacity()).thenReturn(capacityN);
//        when(countMetrics.totalCountMetricCounter()).thenReturn(counter);
//        when(countMetrics.metricCounterIterable(50, TimeUnit.MILLISECONDS)).thenReturn(counters);
//        when(latencyMetrics.intervalSnapshot(TimeoutableResult.SUCCESS)).thenReturn(successLatencySnapshot);
//        when(latencyMetrics.intervalSnapshot(TimeoutableResult.ERROR)).thenReturn(errorLatencySnapshot);
//        when(latencyMetrics.intervalSnapshot(TimeoutableResult.TIMEOUT)).thenReturn(timeoutLatencySnapshot);
//        when(latencyMetrics.latencySnapshot(TimeoutableResult.SUCCESS)).thenReturn(totalSuccessLatencySnapshot);
//        when(latencyMetrics.latencySnapshot(TimeoutableResult.ERROR)).thenReturn(totalErrorLatencySnapshot);
//        when(latencyMetrics.latencySnapshot(TimeoutableResult.TIMEOUT)).thenReturn(totalTimeoutLatencySnapshot);
//
//        registry = new MetricRegistry(50, TimeUnit.MILLISECONDS);
//
//        final AtomicReference<Summary<?>> summaryReference = new AtomicReference<>();
//
//        registry.register(service);
//        registry.setUpdateCallback(new MetricRegistryCallback<Map<String, Summary<?>>>() {
//            @Override
//            public void apply(Map<String, Summary<?>> argument) {
//                summaryReference.compareAndSet(null, argument.get(serviceName));
//                latch.countDown();
//            }
//        });
//
//        latch.await();
//        registry.shutdown();
//
//        Summary<?> summary = summaryReference.get();
//        assertEquals(pendingN, summary.pendingCount);
//        assertEquals(capacityN, summary.remainingCapacity);
//        assertNull(summary.totalResultCounts);
//        assertEquals(errorN, summary.resultCounts);

//        assertEquals(successLatencySnapshot, summary.successLatency);
//        assertEquals(errorLatencySnapshot, summary.errorLatency);
//        assertEquals(timeoutLatencySnapshot, summary.timeoutLatency);
//        assertEquals(totalSuccessLatencySnapshot, summary.totalSuccessLatency);
//        assertEquals(totalErrorLatencySnapshot, summary.totalErrorLatency);
//        assertEquals(totalTimeoutLatencySnapshot, summary.totalTimeoutLatency);
//    }
//
//    private static LatencySnapshot generateSnapshot(Random random) {
//        long startTime = Math.abs(random.nextLong());
//        return new LatencySnapshot(random.nextInt(50), random.nextInt(90), random.nextInt(99), random.nextInt(999),
//                random.nextInt(9999), random.nextInt(99999), random.nextInt(100000), random.nextDouble(), startTime,
//                startTime + 10000L);
//    }
//
//    private static void incrementCounts(AddCounter<TimeoutableResult> counter, TimeoutableResult metric, long n) {
//        for (long i = 0; i < n; ++i) {
//            counter.incrementMetric(metric);
//        }
//    }
}
