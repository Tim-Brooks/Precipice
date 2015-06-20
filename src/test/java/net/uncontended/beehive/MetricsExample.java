package net.uncontended.beehive;

import net.uncontended.beehive.metrics.ActionMetrics;
import net.uncontended.beehive.metrics.DefaultActionMetrics;
import net.uncontended.beehive.metrics.Metric;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by timbrooks on 6/3/15.
 */
public class MetricsExample {

    public static void main(String[] args) {

        DefaultActionMetrics metrics = new DefaultActionMetrics();

        try {
            for (int i = 0; i < 1000; ++i) {
                fireThreads(metrics, 10);
            }

            for (int i = 0; i < 100000; ++i) {
                long start = System.nanoTime();
                metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 5, TimeUnit.SECONDS);
                System.out.println(System.nanoTime() - start);
            }
        } catch (InterruptedException e) {
        }


    }

    private static void fireThreads(final ActionMetrics metrics, int num) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; ++j) {
                        metrics.incrementMetricCount(Metric.SUCCESS);
                        metrics.incrementMetricCount(Metric.ERROR);
                        metrics.incrementMetricCount(Metric.TIMEOUT);
                        metrics.incrementMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED);
                        metrics.incrementMetricCount(Metric.QUEUE_FULL);
                        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN);
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }
}
