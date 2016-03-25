package net.uncontended.precipice.metrics;

public abstract class AbstractMetrics<T extends Enum<T>> implements CountMetrics<T> {
    protected final Class<T> clazz;

    public AbstractMetrics(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> getMetricType() {
        return clazz;
    }
}
