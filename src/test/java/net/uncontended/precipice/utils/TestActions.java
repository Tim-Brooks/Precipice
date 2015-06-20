package net.uncontended.precipice.utils;

import net.uncontended.precipice.ResilientAction;

import java.util.concurrent.CountDownLatch;

/**
 * Created by timbrooks on 1/12/15.
 */
public class TestActions {

    public static ResilientAction<String> blockedAction(final CountDownLatch blockingLatch) {
        return new ResilientAction<String>() {

            @Override
            public String run() throws Exception {
                blockingLatch.await();
                return "Success";
            }
        };
    }

    public static ResilientAction<String> successAction(final long waitTime) {
        return successAction(waitTime, "Success");
    }

    public static ResilientAction<String> successAction(final long waitTime, final String result) {
        return new ResilientAction<String>() {
            @Override
            public String run() throws Exception {
                if (waitTime != 0) {
                    Thread.sleep(waitTime);
                }
                return result;
            }
        };
    }

    public static ResilientAction<String> erredAction(final Exception exception) {
        return new ResilientAction<String>() {
            @Override
            public String run() throws Exception {
                throw exception;
            }
        };
    }
}
