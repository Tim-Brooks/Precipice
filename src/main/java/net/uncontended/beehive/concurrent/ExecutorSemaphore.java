package net.uncontended.beehive.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by timbrooks on 1/12/15.
 */
public class ExecutorSemaphore {

    private final AtomicInteger permitsRemaining;

    public ExecutorSemaphore(int concurrencyLevel) {
        permitsRemaining = new AtomicInteger(concurrencyLevel);
    }

    public boolean acquirePermit() {
        for (; ; ) {
            int permitsRemaining = this.permitsRemaining.get();
            if (permitsRemaining > 0) {
                if (this.permitsRemaining.compareAndSet(permitsRemaining, permitsRemaining - 1)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public void releasePermit() {
        this.permitsRemaining.getAndIncrement();
    }
}
