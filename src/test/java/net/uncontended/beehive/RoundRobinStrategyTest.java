package net.uncontended.beehive;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by timbrooks on 6/15/15.
 */
public class RoundRobinStrategyTest {

    @Test
    public void wrappingWorks() {
        int start = (Integer.MAX_VALUE / 2) - 1;
        RoundRobinStrategy strategy = new RoundRobinStrategy(3, new AtomicInteger(start));

        assertEquals(start % 3, strategy.nextExecutorIndex());
        assertEquals((start + 1) % 3, strategy.nextExecutorIndex());
        assertEquals(0, strategy.nextExecutorIndex());
        assertEquals(1, strategy.nextExecutorIndex());
        assertEquals(2, strategy.nextExecutorIndex());
    }
}
