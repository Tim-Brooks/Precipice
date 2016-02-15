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

import net.uncontended.precipice.PerformingContext;
import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.test_utils.TestResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

public class EventualTest {

    @Mock
    private Completable<TestResult, String> wrappedPromise;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testErrorCallback() throws InterruptedException {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean isTimedOut = new AtomicBoolean(false);

        Eventual<TestResult, String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<TestResult, Throwable>() {
            @Override
            public void apply(TestResult result, Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<TestResult, String>() {
            @Override
            public void apply(TestResult status, String argument) {
                result.set(argument);
            }
        });

        assertTrue(eventual.completeExceptionally(TestResult.ERROR, exception));
        assertFalse(eventual.complete(TestResult.SUCCESS, "NOO"));
        assertFalse(eventual.cancel(true));

        assertSame(exception, error.get());
        assertSame(exception, eventual.getError());
        assertNull(result.get());
        assertNull(eventual.getResult());
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

        Eventual<TestResult, String> eventual = new Eventual<>();


        IOException exception = new IOException();
        eventual.onError(new PrecipiceFunction<TestResult, Throwable>() {
            @Override
            public void apply(TestResult status, Throwable argument) {
                error.set(argument);
            }
        });

        eventual.onSuccess(new PrecipiceFunction<TestResult, String>() {
            @Override
            public void apply(TestResult status, String argument) {
                result.set(argument);
            }
        });

        String stringResult = "YESS";
        assertTrue(eventual.complete(TestResult.SUCCESS, stringResult));
        assertFalse(eventual.completeExceptionally(TestResult.ERROR, exception));
        assertFalse(eventual.cancel(true));

        assertSame(stringResult, result.get());
        assertSame(stringResult, eventual.getResult());
        assertSame(stringResult, eventual.get());
        assertNull(error.get());
        assertNull(eventual.getError());
        assertFalse(isTimedOut.get());
        assertFalse(eventual.isCancelled());
    }

    @Test
    public void testInternalCallbackIsCalledOnError() throws Exception {
        Eventual<TestResult, String> eventual = new Eventual<>();
        final AtomicReference<TestResult> statusReference = new AtomicReference<>();
        final AtomicReference<PerformingContext> context = new AtomicReference<>();

        eventual.internalOnComplete(new PrecipiceFunction<TestResult, PerformingContext>() {
            @Override
            public void apply(TestResult status, PerformingContext argument) {
                statusReference.compareAndSet(null, status);
                context.compareAndSet(null, argument);

            }
        });

        eventual.completeExceptionally(TestResult.ERROR, new RuntimeException());

        assertEquals(TestResult.ERROR, statusReference.get());
        assertSame(eventual, context.get());
    }

    @Test
    public void testInternalCallbackIsCalledOnSuccess() throws Exception {
        Eventual<TestResult, String> eventual = new Eventual<>();
        final AtomicReference<TestResult> statusReference = new AtomicReference<>();
        final AtomicReference<PerformingContext> context = new AtomicReference<>();

        eventual.internalOnComplete(new PrecipiceFunction<TestResult, PerformingContext>() {
            @Override
            public void apply(TestResult status, PerformingContext argument) {
                statusReference.compareAndSet(null, status);
                context.compareAndSet(null, argument);

            }
        });

        eventual.complete(TestResult.SUCCESS, "");

        assertEquals(TestResult.SUCCESS, statusReference.get());
        assertSame(eventual, context.get());
    }

    @Test
    public void testWrappedPromiseIsCompletedOnSuccess() throws Exception {
        Eventual<TestResult, String> eventual = new Eventual<>(wrappedPromise);

        eventual.complete(TestResult.SUCCESS, "Result");

        verify(wrappedPromise).complete(TestResult.SUCCESS, "Result");
    }

    @Test
    public void testWrappedPromiseIsCompletedOnError() throws Exception {
        Eventual<TestResult, String> eventual = new Eventual<>(wrappedPromise);

        RuntimeException ex = new RuntimeException();
        eventual.completeExceptionally(TestResult.ERROR, ex);

        verify(wrappedPromise).completeExceptionally(TestResult.ERROR, ex);
    }
}
