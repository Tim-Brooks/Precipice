package net.uncontended.beehive.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by timbrooks on 6/3/15.
 */
public class ServiceThreadFactory implements ThreadFactory {

    private final AtomicInteger count = new AtomicInteger(0);
    private final String name;


    public ServiceThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name + "-" + count.getAndIncrement());
    }
}
