package net.uncontended.beehive.circuit;

import net.uncontended.beehive.metrics.ActionMetrics;
import net.uncontended.beehive.metrics.HealthSnapshot;
import net.uncontended.beehive.utils.SystemTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by timbrooks on 11/20/14.
 */
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
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig);
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testCircuitOpensOnlyWhenFailuresGreaterThanThreshold() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failingSnapshot = new HealthSnapshot(10000, 6, 0);
        HealthSnapshot healthySnapshot = new HealthSnapshot(10000, 5, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).backOffTimeMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig);

        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(healthySnapshot);
        circuitBreaker.informBreakerOfResult(false);
        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failingSnapshot);
        circuitBreaker.informBreakerOfResult(false);

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testOpenCircuitClosesAfterSuccess() {
        long trailingPeriodInMillis = 1000;
        HealthSnapshot failureSnapshot = new HealthSnapshot(1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(5).trailingPeriodMillis
                (trailingPeriodInMillis).build();
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig);

        assertFalse(circuitBreaker.isOpen());

        when(actionMetrics.healthSnapshot(trailingPeriodInMillis, TimeUnit.MILLISECONDS)).thenReturn(failureSnapshot);
        circuitBreaker.informBreakerOfResult(false);

        assertTrue(circuitBreaker.isOpen());

        circuitBreaker.informBreakerOfResult(true);

        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    public void testSettingBreakerConfigChangesConfig() {
        HealthSnapshot snapshot = new HealthSnapshot(1000, 6, 0);

        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(10).trailingPeriodMillis
                (1000).build();
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig);

        when(actionMetrics.healthSnapshot(1000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        circuitBreaker.informBreakerOfResult(false);
        assertFalse(circuitBreaker.isOpen());

        BreakerConfig newBreakerConfig = new BreakerConfigBuilder().failureThreshold(5)
                .trailingPeriodMillis(2000).build();
        circuitBreaker.setBreakerConfig(newBreakerConfig);

        when(actionMetrics.healthSnapshot(2000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        circuitBreaker.informBreakerOfResult(false);

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    public void testActionAllowedIfCircuitClosed() {
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(20)
                .backOffTimeMillis(5000).build();
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig);
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
        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig, systemTime);

        assertFalse(circuitBreaker.isOpen());
        assertTrue(circuitBreaker.allowAction());

        when(actionMetrics.healthSnapshot(5000, TimeUnit.MILLISECONDS)).thenReturn(snapshot);
        when(systemTime.currentTimeMillis()).thenReturn(0L);
        circuitBreaker.informBreakerOfResult(false);

        when(systemTime.currentTimeMillis()).thenReturn(999L);
        assertFalse(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

        when(systemTime.currentTimeMillis()).thenReturn(1001L);
        assertTrue(circuitBreaker.allowAction());
        assertTrue(circuitBreaker.isOpen());

    }

    @Test
    public void testActionNotAllowedIfCircuitForcedOpen() {
        final int failureThreshold = 10;
        int timePeriodInMillis = 5000;
        BreakerConfig breakerConfig = new BreakerConfigBuilder().failureThreshold(failureThreshold)
                .trailingPeriodMillis(timePeriodInMillis).backOffTimeMillis(1000).build();

        circuitBreaker = new DefaultCircuitBreaker(actionMetrics, breakerConfig, systemTime);

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
