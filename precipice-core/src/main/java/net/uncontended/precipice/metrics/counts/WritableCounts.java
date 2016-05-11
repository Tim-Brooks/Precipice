

package net.uncontended.precipice.metrics.counts;

public interface WritableCounts<T extends Enum<T>> {

    public void write(T result, long number, long nanoTime);

}
