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

package net.uncontended.precipice.core.test_utils;

import net.uncontended.precipice.core.*;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Compliancy {

    public void submitedActionRejectedIfShutdown(SubmissionService service) {
        service.shutdown();

        try {
            service.submit(TestActions.successAction(0), Long.MAX_VALUE);
            fail("Action should have been rejected due to shutdown.");
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.SERVICE_SHUTDOWN, e.reason);
        }
    }

    public void submittedActionNotScheduledIfMaxConcurrencyLevelViolated(SubmissionService service) throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(2);
        service = Services.defaultService("Test", 1, properties);
        CountDownLatch latch = new CountDownLatch(1);
        service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);
        service.submit(TestActions.blockedAction(latch), Long.MAX_VALUE);

        try {
            service.submit(TestActions.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        try {
            service.submit(TestActions.successAction(1), Long.MAX_VALUE);
            fail();
        } catch (RejectedActionException e) {
            assertEquals(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
        latch.countDown();
    }

    public void actionsReleaseSemaphorePermitWhenComplete(SubmissionService service) throws Exception {
        ServiceProperties properties = new ServiceProperties();
        properties.concurrencyLevel(1);
        service = Services.defaultService("Test", 1, properties);
        int iterations = new Random().nextInt(50);
        for (int i = 0; i < iterations; ++i) {
            PrecipiceFuture<String> future = service.submit(TestActions.successAction(1), 500);
            future.get();
            int j = 0;
            while (true) {
                try {
                    service.submit(TestActions.successAction(1), 100);
                    break;
                } catch (RejectedActionException e) {
                    Thread.sleep(5);
                    if (j == 20) {
                        fail("Continue to receive action rejects.");
                    }
                }
                ++j;
            }
        }
    }


}
