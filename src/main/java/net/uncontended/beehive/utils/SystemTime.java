package net.uncontended.beehive.utils;

/**
 * Created by timbrooks on 11/23/14.
 */
public class SystemTime {

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long nanoTime() {
        return System.nanoTime();
    }
}
