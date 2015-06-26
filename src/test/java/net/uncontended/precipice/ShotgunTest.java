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

import net.uncontended.precipice.concurrent.ResilientPromise;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ShotgunTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testShotgunSubmitsCorrectNumberOfTimesRandomly() throws Exception {
        Object context1 = new Object();
        Object context2 = new Object();
        Object context3 = new Object();
        Set<Object> contextsUsed = new HashSet<>();

        for (int i = 0; i < 25; ++i) {
            ArgumentCaptor<Object> contextCaptor = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<ResilientAction> actionCaptor = ArgumentCaptor.forClass(ResilientAction.class);
            MultiService service1 = mock(MultiService.class);
            MultiService service2 = mock(MultiService.class);
            MultiService service3 = mock(MultiService.class);
            ResilientPatternAction<String, Object> patternAction = mock(ResilientPatternAction.class);
            Map<MultiService, Object> services = new HashMap<>();
            services.put(service1, context1);
            services.put(service2, context2);
            services.put(service3, context3);
            Shotgun<Object> shotgun = new Shotgun<>(services, 2);

            shotgun.submit(patternAction, 10);

            verify(service1, atMost(1)).submitAndComplete(actionCaptor.capture(), any(ResilientPromise.class), isNull
                    (ResilientCallback.class), eq(10L));
            verify(service2, atMost(1)).submitAndComplete(actionCaptor.capture(), any(ResilientPromise.class), isNull
                    (ResilientCallback.class), eq(10L));
            verify(service3, atMost(1)).submitAndComplete(actionCaptor.capture(), any(ResilientPromise.class), isNull
                    (ResilientCallback.class), eq(10L));

            List<ResilientAction> actions = actionCaptor.getAllValues();

            assertEquals(2, actions.size());
            for (ResilientAction action : actions) {
                action.run();
            }
            verify(patternAction, times(2)).run(contextCaptor.capture());

            List<Object> contexts = contextCaptor.getAllValues();

            Object contextCaptured1 = contexts.get(0);
            Object contextCaptured2 = contexts.get(1);

            contextsUsed.add(contextCaptured1);
            contextsUsed.add(contextCaptured2);

            assertTrue(contextCaptured1 != contextCaptured2);
        }
        assertEquals(3, contextsUsed.size());
    }

}
