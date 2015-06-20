package net.uncontended.beehive;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by timbrooks on 6/19/15.
 */
public class ShotgunStrategy {

    public final int submissionCount;
    private final int serviceCount;
    private final int[] serviceIndices;

    public ShotgunStrategy(int serviceCount, int submissionCount) {
        this.serviceCount = serviceCount;
        this.submissionCount = submissionCount;
        this.serviceIndices = new int[serviceCount];
        for (int i = 0; i < serviceCount; ++i) {
            serviceIndices[i] = i;
        }
    }

    public int[] executorIndices() {
        int[] orderToTry = new int[serviceCount];

        System.arraycopy(serviceIndices, 0, orderToTry, 0, serviceCount);
        shuffle(orderToTry);

        return orderToTry;
    }

    private void shuffle(int[] orderToTry) {
        int index;
        Random random = ThreadLocalRandom.current();
        for (int i = orderToTry.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
                orderToTry[index] ^= orderToTry[i];
                orderToTry[i] ^= orderToTry[index];
                orderToTry[index] ^= orderToTry[i];
            }
        }
    }
}
