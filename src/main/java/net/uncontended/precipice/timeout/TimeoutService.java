package net.uncontended.precipice.timeout;

import net.uncontended.precipice.concurrent.ResilientPromise;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by timbrooks on 5/30/15.
 */
public class TimeoutService {

    public static final TimeoutService defaultTimeoutService = new TimeoutService("default");
    private final DelayQueue<ActionTimeout> timeoutQueue = new DelayQueue<>();
    private final Thread timeoutThread;
    private AtomicBoolean isStarted = new AtomicBoolean(false);

    public TimeoutService(String name) {
        this.timeoutThread = createThread();
        this.timeoutThread.setName(name + "-timeout-thread");
    }

    public void scheduleTimeout(ActionTimeout timeout) {
        if (!isStarted.get()) {
            startThread();
        }
        timeoutQueue.offer(timeout);
    }

    private void startThread() {
        if (isStarted.compareAndSet(false, true)) {
            timeoutThread.start();
        }
    }

    private Thread createThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    try {
                        ActionTimeout timeout = timeoutQueue.take();
                        @SuppressWarnings("unchecked")
                        ResilientPromise<Object> promise = (ResilientPromise<Object>) timeout.promise;
                        if (promise.setTimedOut()) {
                            timeout.future.cancel(true);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

}
