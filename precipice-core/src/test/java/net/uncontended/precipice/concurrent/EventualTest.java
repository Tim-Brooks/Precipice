/*
 * Copyright 2014 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Status;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class EventualTest {

    @Test
    public void testErrorCallback() throws InterruptedException {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<Status, String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Status, Throwable>() {
            @Override
            public void apply(Status status, Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<Status, String>() {
            @Override
            public void apply(Status status, String argument) {
                result.set(argument);
            }
        });

        assertTrue(eventual.completeExceptionally(Status.ERROR, exception));
        assertFalse(eventual.complete(Status.SUCCESS, "NOO"));
        assertFalse(eventual.cancel(true));

        assertSame(exception, error.get());
        assertSame(exception, eventual.error());
        assertNull(result.get());
        assertNull(eventual.result());
        assertFalse(isTimedOut.get());
        assertFalse(eventual.isCancelled());

        try {
            eventual.get();
        } catch (ExecutionException e) {
            assertSame(exception, e.getCause());
        }
    }

    @Test
    public void testSuccessCallback() throws Exception {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<Status, String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Status, Throwable>() {
            @Override
            public void apply(Status status, Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<Status, String>() {
            @Override
            public void apply(Status status, String argument) {
                result.set(argument);
            }
        });

        String stringResult = "YESS";
        assertTrue(eventual.complete(Status.SUCCESS, stringResult));
        assertFalse(eventual.completeExceptionally(Status.ERROR, exception));
        assertFalse(eventual.cancel(true));

        assertSame(stringResult, result.get());
        assertSame(stringResult, eventual.result());
        assertSame(stringResult, eventual.get());
        assertNull(error.get());
        assertNull(eventual.error());
        assertFalse(isTimedOut.get());
        assertFalse(eventual.isCancelled());
    }

    @Test
    public void testCancellation() throws Exception {
//        final AtomicReference<Throwable> error = new AtomicReference<>();
//        final AtomicReference<String> result = new AtomicReference<>();
//        final AtomicBoolean isTimedOut = new AtomicBoolean(false);
//
//        Eventual<Status, String> eventual = new Eventual<>();
//
//
//        IOException exception = new IOException();
//        eventual.onError(new PrecipiceFunction<Status, Throwable>() {
//            @Override
//            public void apply(Status status, Throwable argument) {
//                error.set(argument);
//            }
//        });
//
//        eventual.onSuccess(new PrecipiceFunction<Status, String>() {
//            @Override
//            public void apply(Status status, String argument) {
//                result.set(argument);
//            }
//        });
//
//        assertTrue(eventual.cancel(true));
//        assertFalse(eventual.complete(Status.SUCCESS, "NOO"));
//        assertFalse(eventual.completeExceptionally(Status.ERROR, exception));
//
//        assertNull(result.get());
//        assertNull(eventual.result());
//        assertNull(error.get());
//        assertNull(eventual.error());
//        assertFalse(isTimedOut.get());
//        assertTrue(eventual.isCancelled());
//
//        try {
//            eventual.get();
//            fail();
//        } catch (CancellationException e) {
//        }
    }
}
