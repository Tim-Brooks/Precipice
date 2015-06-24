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

import net.uncontended.precipice.concurrent.ResilientFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class NewExample {

    public static void main(String[] args) throws InterruptedException {
        String serviceName = "Identity Service";
        int poolSize = 5;
        int concurrencyLevel = 100;
        SubmissionService service = Services.submissionService(serviceName, poolSize, concurrencyLevel);

        int millisTimeout = 10;
        ResilientFuture<Integer> successFuture = service.submit(new SuccessAction(), millisTimeout);

        try {
            // Should return 64
            successFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        assert (successFuture.getStatus() == Status.SUCCESS);

        ResilientFuture<Integer> errorFuture = service.submit(new ErrorAction(), millisTimeout);

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
        ResilientFuture<String> timeoutFuture = service.submit(new TimeoutAction(latch), millisTimeout);

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

    private static class SuccessAction implements ResilientAction<Integer> {

        @Override
        public Integer run() throws Exception {
            return 8 * 8;
        }
    }

    private static class ErrorAction implements ResilientAction<Integer> {

        @Override
        public Integer run() throws Exception {
            throw new RuntimeException("Action failed.");
        }
    }

    private static class TimeoutAction implements ResilientAction<String> {

        private final CountDownLatch latch;

        public TimeoutAction(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String run() throws Exception {
            latch.await();
            return "Done";
        }
    }
}
