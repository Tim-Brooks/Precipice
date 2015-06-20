package net.uncontended.beehive;

import net.uncontended.beehive.concurrent.ResilientPromise;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by timbrooks on 6/11/15.
 */
@SuppressWarnings("unchecked")
public class LoadBalancerPatternTest {

    @Mock
    private Service executor1;
    @Mock
    private Service executor2;
    @Mock
    private LoadBalancerStrategy strategy;
    @Mock
    private ResilientPatternAction<String, Map<String, Object>> action;
    @Captor
    private ArgumentCaptor<ResilientAction<String>> actionCaptor;

    private Map<String, Object> context1;
    private Map<String, Object> context2;
    private LoadBalancerPattern<Map<String, Object>> balancer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        context1 = new HashMap<>();
        context1.put("port", 8000);
        context2 = new HashMap<>();
        context2.put("host", 8001);
        Map<Service, Map<String, Object>> map = new LinkedHashMap<>();
        map.put(executor1, context1);
        map.put(executor2, context2);

        balancer = new LoadBalancerPattern<>(strategy, map);
    }

    @Test
    public void performActionCalledWithCorrectContext() throws Exception {
        when(strategy.nextExecutorIndex()).thenReturn(0);
        balancer.performAction(action);
        verify(executor1).performAction(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context1);

        when(strategy.nextExecutorIndex()).thenReturn(1);
        balancer.performAction(action);
        verify(executor2).performAction(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void submitActionCalledWithCorrectArguments() throws Exception {
        long timeout = 100L;
        ResilientPromise<String> promise = mock(ResilientPromise.class);
        ResilientCallback<String> callback = mock(ResilientCallback.class);

        when(strategy.nextExecutorIndex()).thenReturn(0);
        balancer.submitAction(action, promise, callback, timeout);
        verify(executor1).submitAction(actionCaptor.capture(), eq(promise), eq(callback), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context1);

        when(strategy.nextExecutorIndex()).thenReturn(1);
        balancer.submitAction(action, promise, callback, timeout);
        verify(executor2).submitAction(actionCaptor.capture(), eq(promise), eq(callback), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void submitTriedOnSecondServiceIfRejectedOnFirst() throws Exception {
        long timeout = 100L;
        ResilientPromise<String> promise = mock(ResilientPromise.class);
        ResilientCallback<String> callback = mock(ResilientCallback.class);

        when(strategy.nextExecutorIndex()).thenReturn(0);
        when(executor1.submitAction(actionCaptor.capture(), eq(promise), eq(callback), eq(timeout))).thenThrow(new
                RejectedActionException(RejectionReason.CIRCUIT_OPEN));
        balancer.submitAction(action, promise, callback, timeout);
        verify(executor2).submitAction(actionCaptor.capture(), eq(promise), eq(callback), eq(timeout));
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }

    @Test
    public void actionTriedOnSecondServiceIfRejectedOnFirst() throws Exception {
        when(strategy.nextExecutorIndex()).thenReturn(0);
        when(executor1.performAction(actionCaptor.capture())).thenThrow(new
                RejectedActionException(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED));
        balancer.performAction(action);
        verify(executor2).performAction(actionCaptor.capture());
        actionCaptor.getValue().run();
        verify(action).run(context2);
    }
}
