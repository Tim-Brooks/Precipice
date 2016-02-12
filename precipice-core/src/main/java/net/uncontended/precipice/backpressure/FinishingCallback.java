package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.PerformingContext;
import net.uncontended.precipice.PrecipiceFunction;

class FinishingCallback<Result extends Enum<Result> & Failable> implements PrecipiceFunction<Result,
        PerformingContext> {
    private final GuardRail<Result, ?> guardRail;

    FinishingCallback(GuardRail<Result, ?> guardRail) {
        this.guardRail = guardRail;
    }

    @Override
    public void apply(Result result, PerformingContext context) {
        long endTime = guardRail.getClock().nanoTime();
        guardRail.releasePermits(context.permitCount(), result, context.startNanos(), endTime);
    }
}
