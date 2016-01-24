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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.CompletionContext;
import net.uncontended.precipice.test_utils.TestCallables;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallServiceTest {

    @Mock
    private Controller<Status> controller;
    @Mock
    private CompletionContext<Status, Object> context;

    private CallService service;

    private final ExecutorService actionRunner = Executors.newCachedThreadPool();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new CallService(controller);
    }

    @After
    public void tearDown() {
        service.controller().shutdown();
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(controller.acquirePermitAndGetCompletableContext()).thenThrow(new RejectedException(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.call(TestCallables.success(1));
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(controller.acquirePermitAndGetCompletableContext()).thenThrow(new RejectedException(Rejected.CIRCUIT_OPEN));
            service.call(TestCallables.success(1));
            fail();
        } catch (RejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
    }

    @Test
    public void callableIsExecuted() throws Exception {
        when(controller.acquirePermitAndGetCompletableContext()).thenReturn(context);
        String expectedResult = "Success";

        String result = service.call(TestCallables.success(1));

        verify(context).complete(Status.SUCCESS, expectedResult);

        assertEquals(expectedResult, result);
    }

    @Test
    public void callableExceptionIsHandledAppropriately() throws Exception {
        when(controller.acquirePermitAndGetCompletableContext()).thenReturn(context);

        RuntimeException exception = new RuntimeException();

        try {
            service.call(TestCallables.erred(exception));
        } catch (Exception e) {
            assertEquals(e, exception);
        }

        verify(context).completeExceptionally(Status.ERROR, exception);
    }

    @Test
    public void callableTimeoutExceptionIsHandledAppropriately() throws Exception {
        when(controller.acquirePermitAndGetCompletableContext()).thenReturn(context);

        TimeoutException exception = new PrecipiceTimeoutException();

        try {
            service.call(TestCallables.erred(exception));
        } catch (Exception e) {
            assertEquals(e, exception);
        }

        verify(context).completeExceptionally(Status.TIMEOUT, exception);
    }
}
