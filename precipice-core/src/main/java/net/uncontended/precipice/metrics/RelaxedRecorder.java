package net.uncontended.precipice.metrics;

public abstract class RelaxedRecorder<T extends Enum<T>, V> extends AbstractMetrics<T> implements Recorder<V> {
    protected volatile Holder<V> activeHolder = new Holder<>();
    protected volatile Holder<V> inactiveHolder = new Holder<>();

    public RelaxedRecorder(Class<T> clazz, V initialValue, long nanoTime) {
        super(clazz);
    }

    public V active() {
        return activeHolder.metrics;
    }

    public synchronized V flip(long nanoTime, V newCounter) {
        Holder<V> old = this.activeHolder;
        Holder<V> newHolder = this.inactiveHolder;
        newHolder.metrics = newCounter;
        newHolder.startNanos = nanoTime;
        old.endNanos = nanoTime;
        activeHolder = newHolder;
        inactiveHolder = old;
        return old.metrics;
    }

    protected static class Holder<V> {
        protected long startNanos;
        protected long endNanos;
        protected V metrics;
    }
}
