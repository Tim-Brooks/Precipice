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

package net.uncontended.precipice;

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.concurrent.IntegerSemaphore;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControllerTest {

    private Controller<Status> controller;

    @Test
    public void capacityAndPendingCallsDelegateToSemaphore() {
        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
        properties.semaphore(new IntegerSemaphore(10));
        controller = new Controller<Status>("Controller Name", properties);

        assertEquals(10, controller.remainingCapacity());
        assertEquals(0, controller.pendingCount());

        controller.acquirePermitOrGetRejectedReason();

        assertEquals(9, controller.remainingCapacity());
        assertEquals(1, controller.pendingCount());
    }

    @Test
    public void acquirePermitOrGetRejectedReasonReturnsMaxConcurrency() {
        CircuitBreaker breaker = mock(CircuitBreaker.class);
        PrecipiceSemaphore semaphore = mock(PrecipiceSemaphore.class);

        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
        properties.circuitBreaker(breaker);
        properties.semaphore(semaphore);
        controller = new Controller<Status>("Controller Name", properties);

        when(semaphore.acquirePermit()).thenReturn(true);
        when(breaker.allowAction()).thenReturn(true);

        assertNull(controller.acquirePermitOrGetRejectedReason());

        when(semaphore.acquirePermit()).thenReturn(false);
        when(breaker.allowAction()).thenReturn(true);

        assertSame(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, controller.acquirePermitOrGetRejectedReason());

        when(semaphore.acquirePermit()).thenReturn(true);
        when(breaker.allowAction()).thenReturn(false);

        assertSame(Rejected.CIRCUIT_OPEN, controller.acquirePermitOrGetRejectedReason());


    }

}
