package net.uncontended.beehive.utils;

import net.uncontended.beehive.ResilientCallback;
import net.uncontended.beehive.concurrent.ResilientPromise;

import java.util.concurrent.CountDownLatch;

/**
 * Created by timbrooks on 1/17/15.
 */
public class TestCallbacks {

    public static <T> ResilientCallback<T> completePromiseCallback(final ResilientPromise<ResilientPromise<T>>
                                                                           promiseToComplete) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> promise) {
                promiseToComplete.deliverResult(promise);
            }
        };
    }

    public static <T> ResilientCallback<T> latchedCallback(final CountDownLatch latch) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> resultPromise) {
                latch.countDown();
            }
        };
    }

    public static <T> ResilientCallback<T> exceptionCallback(T type) {
        return new ResilientCallback<T>() {
            @Override
            public void run(ResilientPromise<T> resultPromise) {
                throw new RuntimeException("Boom");
            }
        };
    }
}
