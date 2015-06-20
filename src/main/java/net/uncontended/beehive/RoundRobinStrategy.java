package net.uncontended.beehive;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by timbrooks on 6/15/15.
 */
public class RoundRobinStrategy implements LoadBalancerStrategy {

    private static final int FLIP_POINT = Integer.MAX_VALUE / 2;
    private final int size;
    private final AtomicInteger counter;

    public RoundRobinStrategy(int size) {
        this(size, new AtomicInteger(0));
    }

    public RoundRobinStrategy(int size, AtomicInteger counter) {
        this.size = size;
        this.counter = counter;
    }

    @Override
    public int nextExecutorIndex() {
        int index = counter.getAndIncrement();

        if (index >= FLIP_POINT) {
            resetCounter(index);
        }

        return index % size;
    }

    private void resetCounter(int start) {
        int index = start;
        for (; ; ) {
            if (index < FLIP_POINT) {
                break;
            } else if (counter.compareAndSet(index + 1, 0)) {
                break;
            }
            index = counter.get();

        }
    }
}
