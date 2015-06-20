package net.uncontended.beehive.concurrent;

import net.uncontended.beehive.Status;

/**
 * Created by timbrooks on 12/22/14.
 */
public class DefaultResilientPromise<T> extends AbstractResilientPromise<T> {

    @Override
    public boolean deliverResult(T result) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                this.result = result;
                if (wrappedPromise != null) {
                    wrappedPromise.deliverResult(result);
                }
                latch.countDown();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deliverError(Throwable error) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                this.error = error;
                if (wrappedPromise != null) {
                    wrappedPromise.deliverError(error);
                }
                latch.countDown();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setTimedOut() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                if (wrappedPromise != null) {
                    wrappedPromise.setTimedOut();
                }
                latch.countDown();
                return true;
            }
        }
        return false;
    }

}
