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

import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.HealthSnapshot;
import net.uncontended.precipice.utils.SystemTime;
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
    private ActionMetrics actionMetrics;
    @Mock
    private SystemTime systemTime;

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
        circuitBreaker.setActionMetrics(actionMetrics);
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testCircuitOpensOnlyWhenFailuresGreaterThanThreshold() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failingSnapshot = new HealthSnapshot(10000, 6, 0);
        HealthSnapshot healthySnapshot = new HealthSnapshot(10000, 5, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).backOffTimeMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setActionMetrics(actionMetrics);

        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(healthySnapshot);
        when(systemTime.currentTimeMillis()).thenReturn(501L);
        circuitBreaker.informBreakerOfResult(false);
        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failingSnapshot);
        when(systemTime.currentTimeMillis()).thenReturn(1002L);
        circuitBreaker.informBreakerOfResult(false);
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testOpenCircuitClosesAfterSuccess() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failureSnapshot = new HealthSnapshot(1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).trailingPeriodMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setActionMetrics(actionMetrics);

        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failureSnapshot);
        when(systemTime.currentTimeMillis()).thenReturn(501L);
        circuitBreaker.informBreakerOfResult(false);

        assertTrue(circuitBreaker.isOpen());

        when(systemTime.currentTimeMillis()).thenReturn(501L);
        circuitBreaker.informBreakerOfResult(true);

        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testSettingBreakerConfigChangesConfig() {
        HealthSnapshot snapshot = new HealthSnapshot(1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(10).trailingPeriodMillis
                (1000).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setActionMetrics(actionMetrics);

        when(actionMetrics.healthSnapshot(1000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        when(systemTime.currentTimeMillis()).thenReturn(501L);
        circuitBreaker.informBreakerOfResult(false);
        assertFalse(circuitBreaker.isOpen());

        BreakerConfig newBreakerConfig = new BreakerConfigBuilder().failureThreshold(5)
                .trailingPeriodMillis(2000).build();
        circuitBreaker.setBreakerConfig(newBreakerConfig);

        when(systemTime.currentTimeMillis()).thenReturn(501L);
        circuitBreaker.informBreakerOfResult(false);

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testActionAllowedIfCircuitClosed() {
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(20)
                .backOffTimeMillis(5000).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig);
        circuitBreaker.setActionMetrics(actionMetrics);
        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());
    }

    @Test
    public void testActionAllowedIfPauseTimeHasPassed() {
        int failureThreshold = 10;
        int timePeriodInMillis = 5000;
        HealthSnapshot snapshot = new HealthSnapshot(10000, 11, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(breakerConfig, systemTime);
        circuitBreaker.setActionMetrics(actionMetrics);

        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());

        when(actionMetrics.healthSnapshot(5000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        when(systemTime.currentTimeMillis()).thenReturn(1000L);
        circuitBreaker.informBreakerOfResult(false);

        when(systemTime.currentTimeMillis()).thenReturn(1999L);
        assertFalse(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

        when(systemTime.currentTimeMillis()).thenReturn(2001L);
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
        circuitBreaker.setActionMetrics(actionMetrics);

        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());

        circuitBreaker.forceOpen();

        when(systemTime.currentTimeMillis()).thenReturn(1001L);
        assertFalse(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

        circuitBreaker.forceClosed();

        assertTrue(circuitBreaker.allowAction());
        assertFalse(circuitBreaker.isOpen());

    }

}
