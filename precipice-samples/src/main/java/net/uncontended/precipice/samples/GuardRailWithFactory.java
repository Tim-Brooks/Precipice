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

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.GuardRailBuilder;
import net.uncontended.precipice.concurrent.CompletionContext;
import net.uncontended.precipice.factories.CompletableFactory;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.MetricCounter;
import net.uncontended.precipice.semaphore.UnlimitedSemaphore;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GuardRailWithFactory {

    public static void main(String[] args) {
        CountMetrics<Result> resultMetrics = new MetricCounter<>(Result.class);
        CountMetrics<RejectedReason> rejectedMetrics = new MetricCounter<>(RejectedReason.class);

        GuardRailBuilder<Result, RejectedReason> builder = new GuardRailBuilder<>();
        builder.name("Example")
                .resultMetrics(resultMetrics)
                .rejectedMetrics(rejectedMetrics)
                .addBackPressure(new UnlimitedSemaphore<>(RejectedReason.MAX_CONCURRENCY));

        GuardRail<Result, RejectedReason> guardRail = builder.build();

        CompletionContext<Result, String> completable = CompletableFactory.acquirePermitsAndGetCompletable(guardRail, 1L);

        try {
            URL url = new URL("http://www.google.com");
            URLConnection urlConnection = url.openConnection();
            completable.complete(Result.SUCCESS, readToString(urlConnection.getInputStream()));
        } catch (Exception ex) {
            completable.completeExceptionally(Result.EXCEPTION, ex);
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
