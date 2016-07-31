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

import net.uncontended.precipice.CompletionContext;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.GuardRailBuilder;
import net.uncontended.precipice.factories.Synchronous;
import net.uncontended.precipice.metrics.counts.NoOpCounter;
import net.uncontended.precipice.metrics.counts.TotalCounts;
import net.uncontended.precipice.rejected.Unrejectable;
import net.uncontended.precipice.result.SimpleResult;
import net.uncontended.precipice.semaphore.UnlimitedSemaphore;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public final class GuardRailWithFactory {

    public static void main(String[] args) {
        TotalCounts<SimpleResult> resultMetrics = new TotalCounts<>(SimpleResult.class);
        TotalCounts<Unrejectable> rejectedMetrics = new TotalCounts<>(new NoOpCounter<>(Unrejectable.class));

        GuardRailBuilder<SimpleResult, Unrejectable> builder = new GuardRailBuilder<>();
        builder.name("Example")
                .addResultMetrics(resultMetrics)
                .addRejectedMetrics(rejectedMetrics)
                .addBackPressure(new UnlimitedSemaphore<Unrejectable>());

        GuardRail<SimpleResult, Unrejectable> guardRail = builder.build();

        CompletionContext<SimpleResult, String> completable = Synchronous.acquireSinglePermitAndCompletable(guardRail);

        try {
            URL url = new URL("http://www.google.com");
            URLConnection urlConnection = url.openConnection();
            completable.complete(SimpleResult.SUCCESS, readToString(urlConnection.getInputStream()));
        } catch (Exception ex) {
            completable.completeExceptionally(SimpleResult.ERROR, ex);
        }

        completable.getValue();
        // or
        completable.getError();
    }

    private static String readToString(InputStream inputStream) {
        return "Http Response";
    }

}
