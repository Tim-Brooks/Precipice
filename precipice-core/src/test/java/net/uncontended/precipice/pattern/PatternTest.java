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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.Controllable;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.metrics.ActionMetrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class PatternTest {

    @Mock
    private PatternStrategy strategy;
    @Mock
    private Controllable<Status> controllable1;
    @Mock
    private Controllable<Status> controllable2;
    @Mock
    private Controllable<Status> controllable3;
    @Mock
    private Controller<Status> controller1;
    @Mock
    private Controller<Status> controller2;
    @Mock
    private Controller<Status> controller3;

    private Pattern<Status, Controllable<Status>> pattern;
    private int submissionCount = 2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        List<Controllable<Status>> controllables = Arrays.asList(controllable1, controllable2, controllable3);
        pattern = new Pattern<>(controllables, strategy);

        when(strategy.submissionCount()).thenReturn(submissionCount);
        when(controllable1.controller()).thenReturn(controller1);
        when(controllable2.controller()).thenReturn(controller2);
        when(controllable3.controller()).thenReturn(controller3);
    }

    @Test
    public void getAllReturnsAListOfAllTheChildControllablesInOrder() {
        List<Controllable<Status>> all = pattern.getAllControllables();
        assertEquals(3, all.size());
        assertEquals(controllable1, all.get(0));
        assertEquals(controllable2, all.get(1));
        assertEquals(controllable3, all.get(2));
    }

    @Test
    public void getReturnsCorrectControllables() {
        long nanoTime = 10L;
        int[] indices = {0, 1, 2};

        when(strategy.nextIndices()).thenReturn(indices);
        when(controller1.acquirePermitOrGetRejectedReason()).thenReturn(null);
        when(controller2.acquirePermitOrGetRejectedReason()).thenReturn(null);
        Sequence<Controllable<Status>> all = pattern.getControllables(nanoTime);

        List<Controllable<Status>> controllableList = new ArrayList<>();
        for (Controllable<Status> item : all) {
            controllableList.add(item);
        }

        verifyZeroInteractions(controllable3);
        verifyZeroInteractions(controller3);

        assertEquals(2, controllableList.size());
        assertSame(controllable1, controllableList.get(0));
        assertSame(controllable2, controllableList.get(1));
    }

    @Test
    public void getAcquiresPermitsInTheCorrectOrder() {
        long nanoTime = 10L;
        int[] indices = {2, 0, 1};
        ActionMetrics<Status> metrics = mock(ActionMetrics.class);

        when(strategy.nextIndices()).thenReturn(indices);
        when(controller3.acquirePermitOrGetRejectedReason()).thenReturn(null);
        when(controller1.acquirePermitOrGetRejectedReason()).thenReturn(Rejected.CIRCUIT_OPEN);
        when(controller2.acquirePermitOrGetRejectedReason()).thenReturn(null);
        when(controller1.getActionMetrics()).thenReturn(metrics);

        Sequence<Controllable<Status>> all = pattern.getControllables(nanoTime);

        verify(metrics).incrementRejectionCount(Rejected.CIRCUIT_OPEN, nanoTime);

        List<Controllable<Status>> controllableList = new ArrayList<>();
        for (Controllable<Status> item : all) {
            controllableList.add(item);
        }


        assertEquals(2, controllableList.size());
        assertSame(controllable3, controllableList.get(0));
        assertSame(controllable2, controllableList.get(1));
    }
}
