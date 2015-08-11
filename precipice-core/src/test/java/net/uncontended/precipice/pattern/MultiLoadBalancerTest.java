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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.MultiService;
import net.uncontended.precipice.RejectedActionException;
import net.uncontended.precipice.RejectionReason;
import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.Metric;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class MultiLoadBalancerTest {

    @Mock
    private MultiService executor1;
    @Mock
    private MultiService executor2;
    @Mock
    private LoadBalancerStrategy strategy;
    @Mock
    private ResilientPatternAction<String, Map<String, Object>> action;
    @Mock
    private ActionMetrics metrics;
    @Captor
    private ArgumentCaptor<ResilientAction<String>> actionCaptor;

    private Map<String, Object> context1;
    private Map<String, Object> context2;
    private MultiPattern<Map<String, Object>> balancer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        context1 = new HashMap<>();
        context1.put("port", 8000);
        context2 = new HashMap<>();
        context2.put("host", 8001);
        Map<MultiService, Map<String, Object>> map = new LinkedHashMap<>();
        map.put(executor1, context1);
        map.put(executor2, context2);

        balancer = new MultiLoadBalancer<>(map, strategy, metrics);
    }

    @Test
    public void performActionCalledWithCorrectContext() throws Exception {
        when(strategy.nextExecutorIndex()).thenReturn(0);
        balancer.run(action);
        verify(executor1).run(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context1);

        when(strategy.nextExecutorIndex()).thenReturn(1);
        balancer.run(action);
        verify(executor2).run(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void submitActionCalledWithCorrectArguments() throws Exception {
        long timeout = 100L;
        PrecipicePromise<String> promise = mock(PrecipicePromise.class);
        when(promise.future()).thenReturn(mock(PrecipiceFuture.class));

        when(strategy.nextExecutorIndex()).thenReturn(0);
        balancer.complete(action, promise, timeout);
        verify(executor1).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context1);

        when(strategy.nextExecutorIndex()).thenReturn(1);
        balancer.complete(action, promise, timeout);
        verify(executor2).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void submitTriedOnSecondServiceIfRejectedOnFirst() throws Exception {
        long timeout = 100L;
        PrecipicePromise<String> promise = mock(PrecipicePromise.class);
        when(promise.future()).thenReturn(mock(PrecipiceFuture.class));

        when(strategy.nextExecutorIndex()).thenReturn(0);
        Mockito.doThrow(new RejectedActionException(RejectionReason.CIRCUIT_OPEN)).when(executor1)
                .complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(timeout));
        balancer.complete(action, promise, timeout);
        verify(executor2).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void actionTriedOnSecondServiceIfRejectedOnFirst() throws Exception {
        when(strategy.nextExecutorIndex()).thenReturn(0);
        when(executor1.run(actionCaptor.capture())).thenThrow(new
                RejectedActionException(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED));
        balancer.run(action);
        verify(executor2).run(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void callbackUpdatesMetricsAndCallsUserCallbackForSubmit() throws Exception {
        long timeout = 100L;
        PrecipicePromise<String> promise = mock(PrecipicePromise.class);
        ArgumentCaptor<PrecipicePromise> promiseCaptor = ArgumentCaptor.forClass(PrecipicePromise.class);


        balancer.complete(action, promise, timeout);

        verify(executor1).complete(any(ResilientAction.class), promiseCaptor.capture(), eq(timeout));

        promiseCaptor.getValue().completeExceptionally(new IOException());

        verify(metrics).incrementMetricCount(Metric.ERROR);
    }

    @Test
    public void callbackUpdatesMetricsAndCallsUserCallbackForRun() throws Exception {
        when(strategy.nextExecutorIndex()).thenReturn(0);
        balancer.run(action);
        verify(metrics).incrementMetricCount(Metric.SUCCESS);

        try {
            when(strategy.nextExecutorIndex()).thenReturn(0);
            when(executor1.run(any(ResilientAction.class))).thenThrow(new RuntimeException());
            balancer.run(action);
        } catch (Exception e) {
        }
        verify(metrics).incrementMetricCount(Metric.ERROR);

        try {
            when(strategy.nextExecutorIndex()).thenReturn(1);
            when(executor2.run(any(ResilientAction.class))).thenThrow(new ActionTimeoutException());
            balancer.run(action);
        } catch (ActionTimeoutException e) {
        }
        verify(metrics).incrementMetricCount(Metric.TIMEOUT);
    }

    @Test
    public void scenarioWhereAllServicesReject() {
        long timeout = 100L;
        RejectedActionException rejected = new RejectedActionException(RejectionReason.CIRCUIT_OPEN);

        when(strategy.nextExecutorIndex()).thenReturn(0).thenReturn(1);
        doThrow(rejected).when(executor1).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(timeout));
        doThrow(rejected).when(executor2).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(timeout));

        try {
            balancer.submit(action, timeout);
            fail("Should have been rejected");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.ALL_SERVICES_REJECTED, e.reason);
        }
        verify(metrics).incrementMetricCount(Metric.ALL_SERVICES_REJECTED);
    }

    // TODO: Add tests for other submission methods
}
