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

import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.metrics.RollingCountMetrics;
import net.uncontended.precipice.GuardRailBuilder;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.threadpool.ThreadPoolService;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class SubmissionExample {

    public static void main(String[] args) throws InterruptedException {
        String name = "Identity Service";
        int poolSize = 5;
        int concurrencyLevel = 100;
        GuardRailBuilder<Status, Rejected> builder = new GuardRailBuilder<>();
        builder.name(name)
                .resultMetrics(new RollingCountMetrics<>(Status.class))
                .rejectedMetrics(new RollingCountMetrics<>(Rejected.class));
        ThreadPoolService service = new ThreadPoolService(PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel),
                builder.build());

        int millisTimeout = 10;
        PrecipiceFuture<Status, Integer> successFuture = service.submit(Callables.success(), millisTimeout);

        try {
            // Should return 64
            successFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        assert successFuture.getStatus() == Status.SUCCESS;

        PrecipiceFuture<Status, Integer> errorFuture = service.submit(Callables.error(), millisTimeout);

        try {
            // Should throw ExecutionException
            errorFuture.get();
        } catch (ExecutionException e) {
            // Should be Runtime Exception.
            Throwable cause = e.getCause();
            // Should be "Action Failed."
            cause.getMessage();
        }
        assert errorFuture.getStatus() == Status.ERROR;

        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<Status, Integer> timeoutFuture = service.submit(Callables.timeout(latch), millisTimeout);

        assert timeoutFuture.getStatus() == null;
        try {
            // Should return null
            timeoutFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        // Should return Status.TIMEOUT
        assert timeoutFuture.getStatus() == Status.TIMEOUT;
        latch.countDown();
        service.shutdown();
    }
}
