package net.uncontended.precipice.time;

import org.HdrHistogram.Histogram;

/**
 * Created by timbrooks on 1/1/16.
 */
public class EntryPoint {

    public static void main(String[] args) throws InterruptedException {
        TickingClock clock = TickingClock.getInstance();
        Thread.sleep(5000);
        clock.stop();

        Histogram gram = clock.gram;
        gram.outputPercentileDistribution(System.out, 1000.0);
    }
}
