package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.rejected.Unrejectable;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RollingLatencyTest {

    @Mock
    private Clock systemTime;
    @Mock
    private RollingMetrics<PartitionedLatency<TimeoutableResult>> baseMetrics;
    @Mock
    private PartitionedLatency<TimeoutableResult> histogram;


    private RollingLatency<TimeoutableResult> latencies;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(baseMetrics.current()).thenReturn(histogram);
        when(histogram.getMetricClazz()).thenReturn(TimeoutableResult.class);
        latencies = new RollingLatency<TimeoutableResult>(baseMetrics);
    }

    @Test
    public void clazzComesFromHistogram() {
        assertSame(TimeoutableResult.class, latencies.getMetricClazz());
    }

    @Test
    public void writeUsesBaseMetrics() {
        when(baseMetrics.current(2000L)).thenReturn(histogram);

        latencies.write(TimeoutableResult.ERROR, 5, 300L, 2000L);

        verify(histogram).record(TimeoutableResult.ERROR, 5, 300L);
    }

    @Test
    public void intervalsCallUsesNoOpForDefault() {
        IntervalIterator iterator = mock(IntervalIterator.class);
        when(baseMetrics.intervalsWithDefault(eq(2000L), any(NoOpLatency.class))).thenReturn(iterator);

        IntervalIterator<PartitionedLatency<TimeoutableResult>> intervals = latencies.intervals(2000L);

        assertSame(iterator, intervals);
    }
}
