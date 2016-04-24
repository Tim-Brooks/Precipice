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

package net.uncontended.precipice.circuit;

import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.PartitionedCount;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.counts.NoOpCounter;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.test_utils.TestResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class DefaultCircuitBreakerTest {

    @Mock
    private GuardRail<TestResult, Rejected> guardRail;
    @Mock
    private Rolling<PartitionedCount<TestResult>> countMetrics;
    @Mock
    private HealthGauge healthGauge;

    private CircuitBreakerConfigBuilder<Rejected> builder = new CircuitBreakerConfigBuilder<>(Rejected.CIRCUIT_OPEN);

    private CircuitBreaker<Rejected> circuitBreaker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(guardRail.getResultMetrics()).thenReturn(countMetrics);
        when(countMetrics.current()).thenReturn(new NoOpCounter<>(TestResult.class));
    }

    @Test
    public void testCircuitIsClosedByDefault() {
        CircuitBreakerConfigBuilder<Rejected> bp = builder.failureThreshold(20).backOffTimeMillis(5000);
        CircuitBreakerConfig<Rejected> config = bp.build();
        circuitBreaker = new DefaultCircuitBreaker<>(config);
        circuitBreaker.registerGuardRail(guardRail);
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testCircuitOpensOnlyWhenFailuresGreaterThanThreshold() {
        long trailingPeriodInMillis = 5000;
        HealthSnapshot failingSnapshot = new HealthSnapshot(10000, 6);
        HealthSnapshot healthySnapshot = new HealthSnapshot(10000, 5);

        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(5)
                .backOffTimeMillis(trailingPeriodInMillis)
                .build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        assertFalse(circuitBreaker.isOpen());

        long nanoTime = 501L * 1000L * 1000L;
        when(healthGauge.getHealth(trailingPeriodInMillis, TimeUnit.MILLISECONDS, nanoTime)).thenReturn(healthySnapshot);
        circuitBreaker.releasePermit(1, TestResult.ERROR, nanoTime);
        assertFalse(circuitBreaker.isOpen());

        nanoTime = 1002L * 1000L * 1000L;
        when(healthGauge.getHealth(trailingPeriodInMillis, TimeUnit.MILLISECONDS, nanoTime)).thenReturn(failingSnapshot);
        circuitBreaker.releasePermit(1, TestResult.ERROR, nanoTime);
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testOpenCircuitClosesAfterSuccess() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failureSnapshot = new HealthSnapshot(1000, 6);

        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(5).trailingPeriodMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        assertFalse(circuitBreaker.isOpen());

        long nanoTime = 501L * 1000L * 1000L;
        when(healthGauge.getHealth(trailingPeriodInMillis, TimeUnit.MILLISECONDS, nanoTime)).thenReturn(failureSnapshot);
        circuitBreaker.releasePermit(1L, TestResult.ERROR, nanoTime);

        assertTrue(circuitBreaker.isOpen());

        circuitBreaker.releasePermit(1L, TestResult.SUCCESS, nanoTime);

        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testSettingBreakerConfigChangesConfig() {
        HealthSnapshot snapshot = new HealthSnapshot(1000, 6);

        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(10).trailingPeriodMillis(1000).build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        long nanoTime = 501L * 1000L * 1000L;
        when(healthGauge.getHealth(1000, TimeUnit.MILLISECONDS, nanoTime)).thenReturn(snapshot);
        circuitBreaker.releasePermit(1L, TestResult.ERROR, nanoTime);
        assertFalse(circuitBreaker.isOpen());

        CircuitBreakerConfig<Rejected> newBreakerConfig = builder.failureThreshold(5).trailingPeriodMillis(2000).build();
        circuitBreaker.setBreakerConfig(newBreakerConfig);

        circuitBreaker.releasePermit(1L, TestResult.ERROR, nanoTime);

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testActionAllowedIfCircuitClosed() {
        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(10).backOffTimeMillis(1000).build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        assertFalse(circuitBreaker.isOpen());
        assertNull(circuitBreaker.acquirePermit(1L, 0L));
    }

    @Test
    public void testActionAllowedIfPauseTimeHasPassed() {
        int failureThreshold = 10;
        int timePeriodInMillis = 5000;
        HealthSnapshot snapshot = new HealthSnapshot(10000, 11);

        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        assertFalse(circuitBreaker.isOpen());
        assertNull(circuitBreaker.acquirePermit(1L, 0L));

        long nanoTime = 1000L * 1000L * 1000L;
        when(healthGauge.getHealth(5000, TimeUnit.MILLISECONDS, nanoTime)).thenReturn(snapshot);
        circuitBreaker.releasePermit(1L, TestResult.ERROR, nanoTime);

        nanoTime = 1999L * 1000L * 1000L;
        assertEquals(Rejected.CIRCUIT_OPEN, circuitBreaker.acquirePermit(1L, nanoTime));
        assertTrue(circuitBreaker.isOpen());

        nanoTime = 2001L * 1000L * 1000L;
        assertNull(circuitBreaker.acquirePermit(1L, nanoTime));
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testActionNotAllowedIfCircuitForcedOpen() {
        final int failureThreshold = 10;
        int timePeriodInMillis = 5000;

        CircuitBreakerConfig<Rejected> breakerConfig = builder.failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).backOffTimeMillis(1000).build();
        circuitBreaker = new DefaultCircuitBreaker<>(breakerConfig, healthGauge);
        circuitBreaker.registerGuardRail(guardRail);

        assertFalse(circuitBreaker.isOpen());
        assertNull(circuitBreaker.acquirePermit(1L, 0L));

        circuitBreaker.forceOpen();

        assertEquals(Rejected.CIRCUIT_OPEN, circuitBreaker.acquirePermit(1L, 2L));
        assertTrue(circuitBreaker.isOpen());

        circuitBreaker.forceClosed();

        assertNull(circuitBreaker.acquirePermit(1L, 3L));
        assertFalse(circuitBreaker.isOpen());

    }

}
