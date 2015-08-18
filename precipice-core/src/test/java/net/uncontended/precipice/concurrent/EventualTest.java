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
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CancellationException;
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

        Eventual<String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Throwable>() {
            @Override
            public void apply(Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onTimeout(new PrecipiceFunction<Void>() {
            @Override
            public void apply(Void argument) {
                isTimedOut.set(true);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<String>() {
            @Override
            public void apply(String argument) {
                result.set(argument);
            }
        });

        assertTrue(eventual.completeExceptionally(exception));
        assertFalse(eventual.complete("NOO"));
        assertFalse(eventual.completeWithTimeout());
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
    public void testSuccessCallback() throws InterruptedException, ExecutionException {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Throwable>() {
            @Override
            public void apply(Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onTimeout(new PrecipiceFunction<Void>() {
            @Override
            public void apply(Void argument) {
                isTimedOut.set(true);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<String>() {
            @Override
            public void apply(String argument) {
                result.set(argument);
            }
        });

        String stringResult = "YESS";
        assertTrue(eventual.complete(stringResult));
        assertFalse(eventual.completeExceptionally(exception));
        assertFalse(eventual.completeWithTimeout());
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
    public void testTimeoutCallback() throws InterruptedException, ExecutionException {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Throwable>() {
            @Override
            public void apply(Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onTimeout(new PrecipiceFunction<Void>() {
            @Override
            public void apply(Void argument) {
                isTimedOut.set(true);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<String>() {
            @Override
            public void apply(String argument) {
                result.set(argument);
            }
        });

        assertTrue(eventual.completeWithTimeout());
        assertFalse(eventual.completeExceptionally(exception));
        assertFalse(eventual.complete("NOO"));
        assertFalse(eventual.cancel(true));

        assertNull(result.get());
        assertNull(eventual.result());
        assertNull(error.get());
        assertNull(eventual.error());
        assertNull(eventual.get());
        assertTrue(isTimedOut.get());
        assertFalse(eventual.isCancelled());
    }

    @Test
    public void testCancellation() throws InterruptedException, ExecutionException {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<Throwable>() {
            @Override
            public void apply(Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onTimeout(new PrecipiceFunction<Void>() {
            @Override
            public void apply(Void argument) {
                isTimedOut.set(true);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<String>() {
            @Override
            public void apply(String argument) {
                result.set(argument);
            }
        });

        assertTrue(eventual.cancel(true));
        assertFalse(eventual.complete("NOO"));
        assertFalse(eventual.completeExceptionally(exception));
        assertFalse(eventual.completeWithTimeout());

        assertNull(result.get());
        assertNull(eventual.result());
        assertNull(error.get());
        assertNull(eventual.error());
        assertFalse(isTimedOut.get());
        assertTrue(eventual.isCancelled());

        try {
            eventual.get();
            fail();
        } catch (CancellationException e) {}
    }
}
