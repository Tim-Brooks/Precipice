package net.uncontended.beehive.circuit;

/**
 * Created by timbrooks on 11/10/14.
 */
public class BreakerConfig {

    public final int failurePercentageThreshold;
    public final long failureThreshold;
    public final long trailingPeriodMillis;
    public final long healthRefreshMillis;
    public final long backOffTimeMillis;

    public BreakerConfig(long failureThreshold, int failurePercentageThreshold, long trailingPeriodMillis,
                         long healthRefreshMillis, long backOffTimeMillis) {
        this.failureThreshold = failureThreshold;
        this.failurePercentageThreshold = failurePercentageThreshold;
        this.trailingPeriodMillis = trailingPeriodMillis;
        this.healthRefreshMillis = healthRefreshMillis;
        this.backOffTimeMillis = backOffTimeMillis;
    }

}
