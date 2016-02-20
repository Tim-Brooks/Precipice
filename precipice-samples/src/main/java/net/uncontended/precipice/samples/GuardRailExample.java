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

package net.uncontended.precipice.samples;

import net.uncontended.precipice.*;
import net.uncontended.precipice.metrics.RollingCountMetrics;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.semaphore.LongSemaphore;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GuardRailExample {

    public static void main(String[] args) {
        RollingCountMetrics<Result> resultMetrics = new RollingCountMetrics<>(Result.class);
        RollingCountMetrics<RejectedReason> rejectedMetrics = new RollingCountMetrics<>(RejectedReason.class);

        GuardRailBuilder<Result, RejectedReason> builder = new GuardRailBuilder<>();
        builder.name("Example")
                .resultMetrics(resultMetrics)
                .rejectedMetrics(rejectedMetrics)
                .addBackPressure(new LongSemaphore<>(RejectedReason.MAX_CONCURRENCY, 10));

        GuardRail<Result, RejectedReason> guardRail = builder.build();

        long startNanoTime = guardRail.getClock().nanoTime();
        RejectedReason rejected = guardRail.acquirePermits(1L, startNanoTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }

        String response = null;
        try {
            URL url = new URL("http://www.google.com");
            URLConnection urlConnection = url.openConnection();
            response = readToString(urlConnection.getInputStream());
            guardRail.releasePermits(1L, Result.SUCCESS, startNanoTime);
        } catch (Exception ex) {
            guardRail.releasePermits(1L, Result.EXCEPTION, startNanoTime);
        }
    }

    private static String readToString(InputStream inputStream) {
        return "Http Response";
    }

    public enum Result implements Failable {
        SUCCESS,
        EXCEPTION;

        @Override
        public boolean isFailure() {
            return this == EXCEPTION;
        }

        @Override
        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }

    public enum RejectedReason {
        MAX_CONCURRENCY
    }
}
