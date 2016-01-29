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

import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.AsyncService;
import org.mockito.*;

@SuppressWarnings("unchecked")
public class ShotgunTest {

    private Object context1 = new Object();
    private Object context2 = new Object();
    private Object context3 = new Object();
    @Mock
    private AsyncService service1;
    @Mock
    private AsyncService service2;
    @Mock
    private AsyncService service3;
    @Mock
    private PatternAction<String, Object> patternAction;
    @Mock
    private ShotgunStrategy strategy;
    @Captor
    private ArgumentCaptor<ResilientAction<String>> actionCaptor;
    @Captor
    private ArgumentCaptor<Object> contextCaptor;

    private ThreadPoolShotgun<Object> shotgun;

//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//
//        Map<AsyncService, Object> services = new LinkedHashMap<>();
//        services.put(service1, context1);
//        services.put(service2, context2);
//        services.put(service3, context3);
//        this.shotgun = new ThreadPoolShotgun<>(services, 2, strategy);
//
//        when(strategy.getSubmissionCount()).thenReturn(2);
//    }
//
//    @Test
//    public void actionsSubmittedToServicesAndContextsProvided() throws Exception {
//        int[] indices = {2, 0, 1};
//        when(strategy.executorIndices()).thenReturn(indices);
//        shotgun.submit(patternAction, 100L);
//
//        InOrder inOrder = inOrder(service3, service1);
//        inOrder.verify(service3).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//        inOrder.verify(service1).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//
//        List<ResilientAction<String>> actions = actionCaptor.getAllValues();
//
//        actions.get(0).run();
//        actions.get(1).run();
//        verify(patternAction, times(2)).run(contextCaptor.capture());
//
//
//        List<Object> contexts = contextCaptor.getAllValues();
//        assertEquals(context3, contexts.get(0));
//        assertEquals(context1, contexts.get(1));
//    }
//
//    @Test
//    public void actionsSubmittedToBackupServicesIfRejected() throws Exception {
//        int[] indices = {2, 0, 1};
//        when(strategy.executorIndices()).thenReturn(indices);
//
//        Mockito.doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service1).complete
//                (any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//        shotgun.submit(patternAction, 100L);
//        InOrder inOrder = inOrder(service3, service1, service2);
//        inOrder.verify(service3).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//        inOrder.verify(service1).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//        inOrder.verify(service2).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//
//        List<ResilientAction<String>> actions = actionCaptor.getAllValues();
//
//        actions.get(0).run();
//        actions.get(1).run();
//        verify(patternAction, times(2)).run(contextCaptor.capture());
//
//
//        List<Object> contexts = contextCaptor.getAllValues();
//        assertEquals(context3, contexts.get(0));
//        assertEquals(context2, contexts.get(1));
//    }
//
//    @Test
//    public void submitSucceedsIfAtLeastOnceServiceAccepts() throws Exception {
//        int[] indices = {2, 0, 1};
//        when(strategy.executorIndices()).thenReturn(indices);
//
//        try {
//            doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service3).complete
//                    (any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service1).complete
//                    (any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            shotgun.submit(patternAction, 100L);
//            InOrder inOrder = inOrder(service3, service1, service2);
//            inOrder.verify(service3).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            inOrder.verify(service1).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            inOrder.verify(service2).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//        } catch (RejectedException e) {
//            fail("Action should have been accepted by one service.");
//        }
//    }
//
//    @Test
//    public void submitFailsIfAllServicesReject() throws Exception {
//        int[] indices = {2, 0, 1};
//        when(strategy.executorIndices()).thenReturn(indices);
//
//        try {
//            doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service3).complete
//                    (actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//            doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service1).complete
//                    (actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//            doThrow(new RejectedException(Rejected.CIRCUIT_OPEN)).when(service2).complete
//                    (actionCaptor.capture(), any(PrecipicePromise.class), eq(100L));
//            shotgun.submit(patternAction, 100L);
//            InOrder inOrder = inOrder(service3, service1, service2);
//            inOrder.verify(service3).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            inOrder.verify(service1).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            inOrder.verify(service2).complete(any(ResilientAction.class), any(PrecipicePromise.class), eq(100L));
//            fail();
//        } catch (RejectedException e) {
//            assertEquals(Rejected.ALL_SERVICES_REJECTED, e.reason);
//        }
//    }
//
//    @Test
//    public void shotgunSubmitsCorrectNumberOfTimesToRandomlySelectedServices() throws Exception {
//        Set<Object> contextsUsed = new HashSet<>();
//
//        for (int i = 0; i < 25; ++i) {
//            ArgumentCaptor<Object> contextCaptor = ArgumentCaptor.forClass(Object.class);
//            ArgumentCaptor<ResilientAction> actionCaptor = ArgumentCaptor.forClass(ResilientAction.class);
//            AsyncService service1 = mock(AsyncService.class);
//            AsyncService service2 = mock(AsyncService.class);
//            AsyncService service3 = mock(AsyncService.class);
//            PatternAction<String, Object> patternAction = mock(PatternAction.class);
//            Map<AsyncService, Object> services = new HashMap<>();
//            services.put(service1, context1);
//            services.put(service2, context2);
//            services.put(service3, context3);
//            ThreadPoolShotgun<Object> shotgun = new ThreadPoolShotgun<>(services, 2);
//
//            shotgun.submit(patternAction, 10);
//
//            verify(service1, atMost(1)).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(10L));
//            verify(service2, atMost(1)).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(10L));
//            verify(service3, atMost(1)).complete(actionCaptor.capture(), any(PrecipicePromise.class), eq(10L));
//
//            List<ResilientAction> actions = actionCaptor.getAllValues();
//
//            assertEquals(2, actions.size());
//            for (ResilientAction action : actions) {
//                action.run();
//            }
//            verify(patternAction, times(2)).run(contextCaptor.capture());
//
//            List<Object> contexts = contextCaptor.getAllValues();
//
//            Object contextCaptured1 = contexts.get(0);
//            Object contextCaptured2 = contexts.get(1);
//
//            contextsUsed.add(contextCaptured1);
//            contextsUsed.add(contextCaptured2);
//
//            assertNotSame(contextCaptured1, contextCaptured2);
//        }
//        assertEquals(3, contextsUsed.size());
//    }

}
