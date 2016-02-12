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

import net.uncontended.precipice.backpressure.BPRejectedException;
import net.uncontended.precipice.backpressure.CompletableFactory;
import net.uncontended.precipice.concurrent.CompletionContext;
import net.uncontended.precipice.test_utils.TestCallables;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallServiceTest {

    @Mock
    private GuardRail<Status, Rejected> guardRail;
    @Mock
    private CompletableFactory<Status, Rejected> completableFactory;
    @Mock
    private CompletionContext<Status, Object> context;

    private CallService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new CallService(guardRail, completableFactory);
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(completableFactory.acquirePermitsAndGetCompletable(1L)).thenThrow(new BPRejectedException(Rejected
                    .MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.call(TestCallables.success(1));
            fail();
        } catch (BPRejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(completableFactory.acquirePermitsAndGetCompletable(1L)).thenThrow(new BPRejectedException(Rejected.CIRCUIT_OPEN));
            service.call(TestCallables.success(1));
            fail();
        } catch (BPRejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
    }

    @Test
    public void callableIsExecuted() throws Exception {
        when(completableFactory.acquirePermitsAndGetCompletable(1L)).thenReturn(context);
        String expectedResult = "Success";

        String result = service.call(TestCallables.success(1));

        verify(context).complete(Status.SUCCESS, expectedResult);

        assertEquals(expectedResult, result);
    }

    @Test
    public void callableExceptionIsHandledAppropriately() throws Exception {
        when(completableFactory.acquirePermitsAndGetCompletable(1L)).thenReturn(context);

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
        when(completableFactory.acquirePermitsAndGetCompletable(1L)).thenReturn(context);

        TimeoutException exception = new PrecipiceTimeoutException();

        try {
            service.call(TestCallables.erred(exception));
        } catch (Exception e) {
            assertEquals(e, exception);
        }

        verify(context).completeExceptionally(Status.TIMEOUT, exception);
    }
}
