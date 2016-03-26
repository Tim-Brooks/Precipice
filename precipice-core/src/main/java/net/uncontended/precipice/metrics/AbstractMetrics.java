package net.uncontended.precipice.metrics;

public abstract class AbstractMetrics<T extends Enum<T>> implements IMetric<T> {
    protected final Class<T> clazz;
    private boolean isClosed = false;

    public AbstractMetrics(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> getMetricType() {
        return clazz;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    public void close(long endNanos) {
        this.isClosed = true;
    }
}
