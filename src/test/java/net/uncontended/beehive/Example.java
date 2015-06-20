package net.uncontended.beehive;

import net.uncontended.beehive.circuit.BreakerConfig;
import net.uncontended.beehive.circuit.BreakerConfigBuilder;
import net.uncontended.beehive.circuit.DefaultCircuitBreaker;
import net.uncontended.beehive.metrics.ActionMetrics;
import net.uncontended.beehive.metrics.DefaultActionMetrics;
import net.uncontended.beehive.metrics.Metric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by timbrooks on 11/6/14.
 */
public class Example {

    public static void main(String[] args) {
        ActionMetrics actionMetrics = new DefaultActionMetrics();
        BreakerConfig breakerConfig = new BreakerConfigBuilder().trailingPeriodMillis(5000)
                .failureThreshold(100).backOffTimeMillis(2000).build();
        ServiceExecutor serviceExecutor = Service.defaultService("Test", 25, 120, actionMetrics,
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
            Thread thread = new Thread(new ExampleRequest(serviceExecutor));
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
        serviceExecutor.shutdown();
    }

}
