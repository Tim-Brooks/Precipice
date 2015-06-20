package net.uncontended.precipice.circuit;

/**
 * Created by timbrooks on 6/11/15.
 */
public class BreakerConfigBuilder {
    public long trailingPeriodMillis = 1000;
    public long failureThreshold = Long.MAX_VALUE;
    public int failurePercentageThreshold = 50;
    public long healthRefreshMillis = 500;
    public long backOffTimeMillis = 1000;

    public BreakerConfigBuilder trailingPeriodMillis(long trailingPeriodMillis) {
        this.trailingPeriodMillis = trailingPeriodMillis;
        return this;
    }

    public BreakerConfigBuilder failureThreshold(long failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
    }

    public BreakerConfigBuilder failurePercentageThreshold(int failurePercentageThreshold) {
        this.failurePercentageThreshold = failurePercentageThreshold;
        return this;
    }

    public BreakerConfigBuilder backOffTimeMillis(long backOffTimeMillis) {
        this.backOffTimeMillis = backOffTimeMillis;
        return this;
    }

    public BreakerConfigBuilder healthRefreshMillis(long healthRefreshMillis) {
        this.healthRefreshMillis = healthRefreshMillis;
        return this;
    }

    public BreakerConfig build() {
        return new BreakerConfig(failureThreshold, failurePercentageThreshold, trailingPeriodMillis,
                healthRefreshMillis, backOffTimeMillis);
    }

}
