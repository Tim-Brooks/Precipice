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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ThreadPoolTaskTest {

    @Mock
    private PrecipicePromise<Status, String> promise;

    private ThreadPoolTask<String> task;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void ensureThatTimeoutsAreSetupCorrectly() {
        long millisRelativeTimeout = 10L;
        long nanosStart = 0L;
        task = new ThreadPoolTask<>(TestCallable.success("Success"), promise, millisRelativeTimeout, nanosStart);

        assertEquals(millisRelativeTimeout, task.millisRelativeTimeout);
        assertEquals(millisRelativeTimeout, task.getMillisRelativeTimeout());
        assertEquals(nanosStart + TimeUnit.MILLISECONDS.toNanos(millisRelativeTimeout), task.nanosAbsoluteTimeout);
    }

    @Test
    public void callableNotRunIfFutureAlreadyComplete() {
        PrecipiceFuture<Status, String> future = mock(PrecipiceFuture.class);
        Callable<String> callable = mock(Callable.class);

        task = new ThreadPoolTask<>(callable, promise, 10L, 0L);

        when(promise.future()).thenReturn(future);
        when(future.isDone()).thenReturn(true);

        task.run();

        verify(promise).future();
        verifyNoMoreInteractions(promise);
        verifyZeroInteractions(callable);
    }
}
