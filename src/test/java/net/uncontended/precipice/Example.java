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

import net.uncontended.precipice.circuit.BreakerConfig;
import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.Metric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Example {

    public static void main(String[] args) {
        ActionMetrics actionMetrics = new DefaultActionMetrics();
        BreakerConfig breakerConfig = new BreakerConfigBuilder().trailingPeriodMillis(5000)
                .failureThreshold(100).backOffTimeMillis(2000).build();
        MultiService service = Services.defaultService("Test", 25, 120, actionMetrics,
                new DefaultCircuitBreaker(actionMetrics, breakerConfig));
//        BreakerConfig breakerConfig2 = new BreakerConfig.BreakerConfigBuilder().trailingPeriodInMillis(5000)
//                .failureThreshold(400).backOffTimeInMillis(2000).build();
//        SingleWriterActionMetrics actionMetrics2 = new SingleWriterActionMetrics(3600);
//        ServiceExecutor serviceExecutor2 = new EventLoopExecutor(15, actionMetrics2, new DefaultCircuitBreaker
//                (actionMetrics2, breakerConfig2), Executors.newFixedThreadPool(15));
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 3; ++i) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Thread thread = new Thread(new ExampleRequest(service));
            threads.add(thread);
            thread.start();
        }
//        for (int i = 0; i < 3; ++i) {
//            try {
//                Thread.sleep(30);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            Thread thread = new Thread(new ExampleRequest(serviceExecutor2));
//            threads.add(thread);
//            thread.start();
//        }
        try {
            for (int i = 0; i < 1000; ++i) {
                Thread.sleep(1000);
                System.out.println("Success " + actionMetrics.getMetricCountForTimePeriod(Metric.SUCCESS, 1, TimeUnit.SECONDS));
                System.out.println("Failures " + actionMetrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 1, TimeUnit.SECONDS));
                System.out.println("Errors " + actionMetrics.getMetricCountForTimePeriod(Metric.ERROR, 1, TimeUnit.SECONDS));
                System.out.println("Concurrency " + actionMetrics.getMetricCountForTimePeriod(Metric
                        .MAX_CONCURRENCY_LEVEL_EXCEEDED, 1, TimeUnit.SECONDS));
                System.out.println("Circuit " + actionMetrics.getMetricCountForTimePeriod(Metric.CIRCUIT_OPEN, 1, TimeUnit.SECONDS));
//                System.out.println("Success2 " + actionMetrics2.getSuccessesForTimePeriod(5000));
//                System.out.println("Failures2 " + actionMetrics2.getMetricCountForTimePeriod(5000));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Thread t : threads) {
            t.interrupt();
        }
        service.shutdown();
    }

}
