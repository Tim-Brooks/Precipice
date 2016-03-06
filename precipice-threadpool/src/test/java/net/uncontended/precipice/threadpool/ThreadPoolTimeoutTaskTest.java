/*
 * Copyright 2016 Timothy Brooks
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

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ThreadPoolTimeoutTaskTest {

    @Mock
    private PrecipicePromise<TimeoutableResult, String> promise;
    @Mock
    private PrecipiceFuture<TimeoutableResult, String> future;

    private ThreadPoolTimeoutTask<String> task;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(promise.future()).thenReturn(future);
    }

    @Test
    public void ensureThatTimeoutsAreSetupCorrectly() {
        long millisRelativeTimeout = 10L;
        long nanosStart = 0L;
        task = new ThreadPoolTimeoutTask<>(TestCallable.success("Success"), promise, millisRelativeTimeout, nanosStart);

        assertEquals(millisRelativeTimeout, task.millisRelativeTimeout);
        assertEquals(millisRelativeTimeout, task.getMillisRelativeTimeout());
        assertEquals(nanosStart + TimeUnit.MILLISECONDS.toNanos(millisRelativeTimeout), task.nanosAbsoluteTimeout);
    }

    @Test
    public void callableNotRunIfFutureAlreadyDone() {
        Callable<String> callable = mock(Callable.class);

        task = new ThreadPoolTimeoutTask<>(callable, promise, 10L, 0L);

        when(future.isDone()).thenReturn(true);

        task.run();

        verify(promise).future();
        verifyNoMoreInteractions(promise);
        verifyZeroInteractions(callable);
    }

    @Test
    public void promiseCompletedWithCallableResult() {
        task = new ThreadPoolTimeoutTask<>(TestCallable.success("Success"), promise, 10L, 0L);

        task.run();

        verify(promise).complete(TimeoutableResult.SUCCESS, "Success");

        PrecipicePromise<TimeoutableResult, Integer> promise2 = mock(PrecipicePromise.class);
        ThreadPoolTimeoutTask<Integer> task2 = new ThreadPoolTimeoutTask<>(TestCallable.success(1), promise2, 10L, 0L);

        when(promise2.future()).thenReturn(mock(PrecipiceFuture.class));

        task2.run();

        verify(promise2).complete(TimeoutableResult.SUCCESS, 1);
    }

    @Test
    public void promiseNotTimedOutIfCompleted() {
        task = new ThreadPoolTimeoutTask<>(TestCallable.success("Success"), promise, 10L, 0L);

        task.run();

        verify(promise).future();
        verify(promise).complete(TimeoutableResult.SUCCESS, "Success");

        task.setTimedOut();

        verifyNoMoreInteractions(promise);
    }

    @Test
    public void promiseTimedOutIfNotCompleted() {
        task = new ThreadPoolTimeoutTask<>(TestCallable.success("Success"), promise, 10L, 0L);

        task.setTimedOut();

        verify(promise).completeExceptionally(same(TimeoutableResult.TIMEOUT), any(PrecipiceTimeoutException.class));
    }

    @Test
    public void callableNotRunIfTaskInternallyComplete() {
        task = new ThreadPoolTimeoutTask<>(TestCallable.success("Success"), promise, 10L, 0L);

        task.setTimedOut();

        verify(promise).completeExceptionally(same(TimeoutableResult.TIMEOUT), any(PrecipiceTimeoutException.class));

        task.run();

        verifyNoMoreInteractions(promise);
    }
}
