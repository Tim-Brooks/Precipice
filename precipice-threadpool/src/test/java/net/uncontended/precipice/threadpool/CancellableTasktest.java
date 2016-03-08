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

import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.threadpool.utils.TaskFactory;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.*;

public class CancellableTaskTest {

    @Mock
    private PrecipicePromise<TimeoutableResult, String> promise;
    @Mock
    private PrecipiceFuture<TimeoutableResult, String> future;

    private CancellableTask<TimeoutableResult, String> task;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(promise.future()).thenReturn(future);
    }

    @Test
    public void callableNotRunIfFutureAlreadyDone() {
        Callable<String> callable = mock(Callable.class);

        task = TaskFactory.createTask(callable, promise);

        when(future.isDone()).thenReturn(true);

        task.run();

        verify(promise).future();
        verifyNoMoreInteractions(promise);
        verifyZeroInteractions(callable);
    }

    @Test
    public void promiseCompletedWithCallableResult() {
        task = TaskFactory.createTask(TestCallable.success("Success"), promise);

        task.run();

        verify(promise).complete(TimeoutableResult.SUCCESS, "Success");
    }

    @Test
    public void promiseNotTimedOutIfCompleted() {
        task = TaskFactory.createTask(TestCallable.success("Success"), promise);

        task.run();

        verify(promise).future();
        verify(promise).complete(TimeoutableResult.SUCCESS, "Success");

        task.cancel(TimeoutableResult.TIMEOUT, new PrecipiceTimeoutException());

        verifyNoMoreInteractions(promise);
    }

    @Test
    public void promiseTimedOutIfNotCompleted() {
        task = TaskFactory.createTask(TestCallable.success("Success"), promise);

        PrecipiceTimeoutException exception = new PrecipiceTimeoutException();
        task.cancel(TimeoutableResult.TIMEOUT, exception);

        verify(promise).completeExceptionally(same(TimeoutableResult.TIMEOUT), same(exception));
    }

    @Test
    public void callableNotRunIfTaskInternallyComplete() {
        task = TaskFactory.createTask(TestCallable.success("Success"), promise);

        task.cancel(TimeoutableResult.TIMEOUT, new PrecipiceTimeoutException());

        verify(promise).completeExceptionally(same(TimeoutableResult.TIMEOUT), any(PrecipiceTimeoutException.class));

        task.run();

        verifyNoMoreInteractions(promise);
    }

    @Test
    public void canConfigureStatusMappers() {
        CancellableTask.ResultToStatus<TimeoutableResult, Object> resultToStatus =
                new CancellableTask.ResultToStatus<TimeoutableResult, Object>() {
                    @Override
                    public TimeoutableResult resultToStatus(Object o) {
                        return TimeoutableResult.ERROR;
                    }
                };

        CancellableTask.ThrowableToStatus<TimeoutableResult> throwableToStatus =
                new CancellableTask.ThrowableToStatus<TimeoutableResult>() {
                    @Override
                    public TimeoutableResult throwableToStatus(Throwable t) {
                        return TimeoutableResult.SUCCESS;
                    }
                };

        PrecipicePromise<TimeoutableResult, Object> successPromise = mock(PrecipicePromise.class);
        when(successPromise.future()).thenReturn(mock(PrecipiceFuture.class));

        Object result = new Object();
        CancellableTask<TimeoutableResult, Object> cancellableTask = new CancellableTask<>(resultToStatus,
                throwableToStatus, TestCallable.success(result), successPromise);

        cancellableTask.run();

        verify(successPromise).complete(TimeoutableResult.ERROR, result);

        PrecipicePromise<TimeoutableResult, Object> errorPromise = mock(PrecipicePromise.class);
        when(errorPromise.future()).thenReturn(mock(PrecipiceFuture.class));

        final IOException exception = new IOException();
        CancellableTask<TimeoutableResult, Object> cancellableTask2 = new CancellableTask<>(resultToStatus,
                throwableToStatus, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw exception;
            }
        }, errorPromise);

        cancellableTask2.run();

        verify(errorPromise).completeExceptionally(TimeoutableResult.SUCCESS, exception);
    }
}
