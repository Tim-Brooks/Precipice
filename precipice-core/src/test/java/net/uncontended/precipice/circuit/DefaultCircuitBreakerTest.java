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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.HealthSnapshot;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DefaultCircuitBreakerTest {

    @Mock
    private CountMetrics<Status> countMetrics;
    @Mock
    private Clock systemTime;

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCircuitIsClosedByDefault() {
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(20)
                .backOffTimeMillis(5000).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig);
        circuitBreaker.setCountMetrics(countMetrics);
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testCircuitOpensOnlyWhenFailuresGreaterThanThreshold() {
        long trailingPeriodInMillis = 5000;
        HealthSnapshot failingSnapshot = new HealthSnapshot(10000, 10000, 6, 0);
        HealthSnapshot healthySnapshot = new HealthSnapshot(10000, 10000, 5, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).backOffTimeMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setCountMetrics(countMetrics);

        assertFalse(circuitBreaker.isOpen());

        when(countMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(healthySnapshot);
        when(systemTime.nanoTime()).thenReturn(501L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);
        assertFalse(circuitBreaker.isOpen());

        when(countMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failingSnapshot);
        when(systemTime.nanoTime()).thenReturn(1002L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testOpenCircuitClosesAfterSuccess() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failureSnapshot = new HealthSnapshot(1000, 1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).trailingPeriodMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setCountMetrics(countMetrics);

        assertFalse(circuitBreaker.isOpen());

        when(countMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failureSnapshot);
        when(systemTime.nanoTime()).thenReturn(501L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);

        assertTrue(circuitBreaker.isOpen());

        when(systemTime.nanoTime()).thenReturn(501L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.SUCCESS);

        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testSettingBreakerConfigChangesConfig() {
        HealthSnapshot snapshot = new HealthSnapshot(1000, 1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(10).trailingPeriodMillis
                (1000).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setCountMetrics(countMetrics);

        when(countMetrics.healthSnapshot(1000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        when(systemTime.nanoTime()).thenReturn(501L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);
        assertFalse(circuitBreaker.isOpen());

        BreakerConfig newBreakerConfig = new BreakerConfigBuilder().failureThreshold(5)
                .trailingPeriodMillis(2000).build();
        circuitBreaker.setBreakerConfig(newBreakerConfig);

        when(systemTime.nanoTime()).thenReturn(501L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testActionAllowedIfCircuitClosed() {
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(20)
                .backOffTimeMillis(5000).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig);
        circuitBreaker.setCountMetrics(countMetrics);
        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());
    }

    @Test
    public void testActionAllowedIfPauseTimeHasPassed() {
        int failureThreshold = 10;
        int timePeriodInMillis = 5000;
        HealthSnapshot snapshot = new HealthSnapshot(10000, 10000, 11, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setCountMetrics(countMetrics);

        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());

        when(countMetrics.healthSnapshot(5000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        circuitBreaker.informBreakerOfResult(Status.ERROR);

        when(systemTime.nanoTime()).thenReturn(1999L * 1000L * 1000L);
        assertFalse(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

        when(systemTime.nanoTime()).thenReturn(2001L * 1000L * 1000L);
        assertTrue(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

    }

    @Test
    public void testActionNotAllowedIfCircuitForcedOpen() {
        final int failureThreshold = 10;
        int timePeriodInMillis = 5000;
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).backOffTimeMillis(1000).build();

        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setCountMetrics(countMetrics);

        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());

        circuitBreaker.forceOpen();

        when(systemTime.nanoTime()).thenReturn(1001L * 1000L * 1000L);
        assertFalse(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

        circuitBreaker.forceClosed();

        assertTrue(circuitBreaker.allowAction());
        assertFalse(circuitBreaker.isOpen());

    }

}
