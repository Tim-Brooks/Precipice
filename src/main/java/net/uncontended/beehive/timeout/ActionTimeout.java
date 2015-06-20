package net.uncontended.beehive.timeout;

import net.uncontended.beehive.concurrent.ResilientPromise;

import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by timbrooks on 5/30/15.
 */
public class ActionTimeout implements Delayed {

    public final long millisAbsoluteTimeout;
    public final Future<Void> future;
    public final ResilientPromise<?> promise;

    public ActionTimeout(long millisRelativeTimeout, ResilientPromise<?> promise, Future<Void>
            future) {
        this.millisAbsoluteTimeout = millisRelativeTimeout + System.currentTimeMillis();
        this.promise = promise;
        this.future = future;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(millisAbsoluteTimeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ActionTimeout) {
            return Long.compare(millisAbsoluteTimeout, ((ActionTimeout) o).millisAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
