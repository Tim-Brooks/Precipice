package net.uncontended.precipice.metrics;

public abstract class AbstractMetrics<T extends Enum<T>> implements IMetric<T> {
    protected final Class<T> clazz;

    public AbstractMetrics(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> getMetricType() {
        return clazz;
    }

    @Override
    public boolean isNoOp() {
        return false;
    }

    @Override
    public long startNanos() {
        return 0L;
    }

    @Override
    public long endNanos() {
        return 0L;
    }
}
