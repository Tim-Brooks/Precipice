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

package net.uncontended.precipice.example;

import net.uncontended.precipice.CompletionService;
import net.uncontended.precipice.Services;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.DefaultResilientPromise;
import net.uncontended.precipice.concurrent.ResilientPromise;

import java.util.concurrent.CountDownLatch;

public class CompletingExample {

    public static void main(String[] args) throws InterruptedException {
        String serviceName = "Identity Service";
        int poolSize = 5;
        int concurrencyLevel = 100;
        CompletionService service = Services.completionService(serviceName, poolSize, concurrencyLevel);

        int millisTimeout = 10;
        ResilientPromise<Integer> successPromise = new DefaultResilientPromise<>();
        service.submitAndComplete(Actions.successAction(), successPromise, millisTimeout);

        // Should return 64
        successPromise.awaitResult();

        assert (successPromise.getStatus() == Status.SUCCESS);

        ResilientPromise<Integer> errorPromise = new DefaultResilientPromise<>();
        service.submitAndComplete(Actions.errorAction(), errorPromise, millisTimeout);

        // Should return null
        errorPromise.awaitResult();
        // Should be Runtime Exception.
        Exception cause = errorPromise.getError();
        // Should be "Action Failed."
        cause.getMessage();

        assert (errorPromise.getStatus() == Status.ERROR);

        CountDownLatch latch = new CountDownLatch(1);
        ResilientPromise<Integer> timeoutPromise = new DefaultResilientPromise<>();
        service.submitAndComplete(Actions.timeoutAction(latch), timeoutPromise, millisTimeout);

        assert (timeoutPromise.getStatus() == Status.PENDING);

        // Should return null
        timeoutPromise.awaitResult();

        assert (timeoutPromise.getStatus() == Status.TIMEOUT);

        latch.countDown();
        service.shutdown();
    }

}
