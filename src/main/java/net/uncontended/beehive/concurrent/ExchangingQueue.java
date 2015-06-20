package net.uncontended.beehive.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by timbrooks on 11/11/14.
 */
public class ExchangingQueue<T> {

    private final T[] queue;
    private final int capacity;
    private final int mask;
    private final AtomicInteger head = new AtomicInteger(0);
    private final AtomicInteger tail = new AtomicInteger(0);
    private volatile Thread waiter = null;

    @SuppressWarnings("unchecked")
    public ExchangingQueue(final int capacity) {
        this.capacity = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        this.mask = this.capacity - 1;
        this.queue = (T[]) new Object[this.capacity];
    }

    public boolean offer(final T element) {
        if (null == element) {
            throw new NullPointerException("Cannot put null in the queue");
        }
        final int currentTail = tail.get();
        final int wrapPoint = currentTail - capacity;

        if (head.get() <= wrapPoint) {
            return false;
        }
        queue[currentTail & mask] = element;
        tail.lazySet(currentTail + 1);
        if (waiter != null) {
            LockSupport.unpark(waiter);
        }
        return true;
    }

    public T poll() {
        final int currentHead = head.get();

        if (currentHead >= tail.get()) {
            return null;
        }

        int index = currentHead & mask;
        final T element = queue[index];
        queue[index] = null;
        head.lazySet(currentHead + 1);
        return element;

    }

    public T blockingPoll() throws InterruptedException {
        for (;;) {
            T element = poll();
            if (element != null) {
                return element;
            }
            waiter = Thread.currentThread();
            LockSupport.park();
            waiter = null;
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
    }

}
