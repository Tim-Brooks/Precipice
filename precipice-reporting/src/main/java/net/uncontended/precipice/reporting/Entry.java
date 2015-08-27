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

package net.uncontended.precipice.reporting;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;

import static io.undertow.Handlers.resource;

public class Entry {

    public static void main(String[] args) {
//        PathHandler path = new PathHandler()
//                .addPrefixPath("/", resource(new ClassPathResourceManager(Dashboard.class.getClassLoader(),
//                        Dashboard.class.getPackage())).addWelcomeFiles("chart.html"))
//                .addExactPath("/api", new Dashboard());
//
//        Undertow server = Undertow.builder()
//                .addHttpListener(6001, "127.0.0.1")
//                .setHandler(path)
//                .build();
//        server.start();
//
//        try {
//            Thread.sleep(1000000000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        server.stop();

        Histogram histogram = new Histogram(1000, TimeUnit.HOURS.toNanos(1), 2);
        histogram.recordValue(2);
        histogram.recordValue(2);
        histogram.recordValue(2);
        histogram.recordValue(2);
        histogram.recordValue(2000);
        histogram.recordValue(3000);
        histogram.recordValue(5000);
        histogram.recordValue(TimeUnit.HOURS.toNanos(1));

        System.out.println(histogram.getValueAtPercentile(50.0));
        System.out.println(histogram.getValueAtPercentile(90.0));
        System.out.println(histogram.getValueAtPercentile(99.0));
        System.out.println(histogram.getValueAtPercentile(99.9));
        System.out.println(histogram.getValueAtPercentile(99.99));
        System.out.println(histogram.getValueAtPercentile(99.999));
        System.out.println(histogram.getMaxValue());

        histogram.outputPercentileDistribution(System.out, 1000.0);
    }
}
