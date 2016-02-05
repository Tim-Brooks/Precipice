package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.Result;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class GRProperties<Res extends Enum<Res> & Result, Rejected extends Enum<Rejected>> {

    private final Class<Res> resultType;
    private final Class<Rejected> rejectedType;
    private BPCountMetrics<Res> resultMetrics;
    private BPCountMetrics<Rejected> rejectedMetrics;
    private LatencyMetrics<Res> latencyMetrics;
    private Clock clock = new SystemTime();

    public GRProperties(Class<Res> resultType, Class<Rejected> rejectedType) {
        this.resultType = resultType;
        this.rejectedType = rejectedType;
        resultMetrics = new BPCountMetrics<>(resultType);
        rejectedMetrics = new BPCountMetrics<>(rejectedType);
        latencyMetrics = new IntervalLatencyMetrics<>(resultType);
    }

    public GRProperties<Res, Rejected> resultMetrics(BPCountMetrics<Res> resultMetrics) {
        this.resultMetrics = resultMetrics;
        return this;
    }

    public BPCountMetrics<Res> resultMetrics() {
        return resultMetrics;
    }


    public GRProperties<Res, Rejected> latencyMetrics(LatencyMetrics<Res> latencyMetrics) {
        this.latencyMetrics = latencyMetrics;
        return this;
    }

    public LatencyMetrics<Res> latencyMetrics() {
        return latencyMetrics;
    }

    public Clock clock() {
        return clock;
    }

    public void clock(Clock clock) {
        this.clock = clock;
    }
}
