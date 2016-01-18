package net.uncontended.precipice.timeout;

import java.util.concurrent.Delayed;

public interface TimeoutTask extends Delayed {
    void setTimedOut();

    long getMillisRelativeTimeout();
}
