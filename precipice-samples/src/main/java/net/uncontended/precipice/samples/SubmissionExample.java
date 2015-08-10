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

package net.uncontended.precipice.samples;

import net.uncontended.precipice.core.Services;
import net.uncontended.precipice.core.Status;
import net.uncontended.precipice.core.AsyncService;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class SubmissionExample {

    public static void main(String[] args) throws InterruptedException {
        String serviceName = "Identity Service";
        int poolSize = 5;
        int concurrencyLevel = 100;
        AsyncService service = Services.submissionService(serviceName, poolSize, concurrencyLevel);

        int millisTimeout = 10;
        PrecipiceFuture<Integer> successFuture = service.submit(Actions.successAction(), millisTimeout);

        try {
            // Should return 64
            successFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        assert (successFuture.getStatus() == Status.SUCCESS);

        PrecipiceFuture<Integer> errorFuture = service.submit(Actions.errorAction(), millisTimeout);

        try {
            // Should throw ExecutionException
            errorFuture.get();
        } catch (ExecutionException e) {
            // Should be Runtime Exception.
            Throwable cause = e.getCause();
            // Should be "Action Failed."
            cause.getMessage();
        }
        assert (errorFuture.getStatus() == Status.ERROR);

        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<Integer> timeoutFuture = service.submit(Actions.timeoutAction(latch), millisTimeout);

        assert (timeoutFuture.getStatus() == Status.PENDING);
        try {
            // Should return null
            timeoutFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        // Should return Status.TIMEOUT
        assert (timeoutFuture.getStatus() == Status.TIMEOUT);
        latch.countDown();
        service.shutdown();
    }
}
