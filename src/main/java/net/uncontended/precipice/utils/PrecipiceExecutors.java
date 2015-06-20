package net.uncontended.precipice.utils;

import net.uncontended.precipice.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by timbrooks on 6/20/15.
 */
public class PrecipiceExecutors {

    public static ExecutorService threadPoolExecutor(String name, int poolSize, int concurrencyLevel) {
        if (concurrencyLevel > Service.MAX_CONCURRENCY_LEVEL) {
            throw new IllegalArgumentException("Concurrency Level \"" + concurrencyLevel + "\" is greater than the " +
                    "allowed maximum: " + Service.MAX_CONCURRENCY_LEVEL + ".");
        }
        return new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.DAYS,
                new ArrayBlockingQueue<Runnable>(concurrencyLevel * 2), new ServiceThreadFactory(name));
    }
}
