package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.Status;

/**
 * Created by timbrooks on 11/16/14.
 */
public class SingleWriterResilientPromise<T> extends AbstractResilientPromise<T> {
    @Override
    public boolean deliverResult(T result) {
        this.result = result;
        status.set(Status.SUCCESS);
        latch.countDown();
        return true;
    }

    @Override
    public boolean deliverError(Throwable error) {
        this.error = error;
        status.set(Status.ERROR);
        latch.countDown();
        return true;
    }

    @Override
    public boolean setTimedOut() {
        status.set(Status.TIMEOUT);
        latch.countDown();
        return true;
    }

}
