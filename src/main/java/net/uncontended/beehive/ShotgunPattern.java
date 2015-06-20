package net.uncontended.beehive;

import net.uncontended.beehive.concurrent.DefaultResilientPromise;
import net.uncontended.beehive.concurrent.ResilientFuture;
import net.uncontended.beehive.concurrent.ResilientPromise;

import java.util.Map;

/**
 * Created by timbrooks on 6/16/15.
 */
public class ShotgunPattern<C> implements Pattern<C> {

    private final ServiceExecutor[] services;
    private final ShotgunStrategy strategy;
    private final C[] contexts;

    @SuppressWarnings("unchecked")
    public ShotgunPattern(Map<ServiceExecutor, C> executorToContext, int submissionCount) {
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create Shotgun with 0 Executors.");
        } else if (submissionCount > executorToContext.size()) {
            throw new IllegalArgumentException("Submission count cannot be fewer than number of services provided.");
        }

        services = new ServiceExecutor[executorToContext.size()];
        contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<ServiceExecutor, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }

        this.strategy = new ShotgunStrategy(services.length, submissionCount);
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, long millisTimeout) {
        return submitAction(action, new DefaultResilientPromise<T>(), null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientCallback<T> callback,
                                               long millisTimeout) {
        return submitAction(action, new DefaultResilientPromise<T>(), callback, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                               long millisTimeout) {
        return submitAction(action, promise, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                               ResilientCallback<T> callback, long millisTimeout) {
        final int[] servicesToTry = strategy.executorIndices();
        ResilientActionWithContext<T, C> actionWithContext = new ResilientActionWithContext<>(action);

        int submittedCount = 0;
        for (int serviceIndex : servicesToTry) {
            try {
                actionWithContext.context = contexts[serviceIndex];
                services[serviceIndex].submitAction(actionWithContext, promise, callback, millisTimeout);
                ++submittedCount;
            } catch (RejectedActionException e) {
            }
            if (submittedCount == strategy.submissionCount) {
                break;
            }
        }
        if (submittedCount == 0) {
            throw new RejectedActionException(RejectionReason.ALL_SERVICES_REJECTED);
        }
        return new ResilientFuture<>(promise);
    }

    @Override
    public <T> ResilientPromise<T> performAction(ResilientPatternAction<T, C> action) {
        return null;
    }

    @Override
    public void shutdown() {
        for (ServiceExecutor service : services) {
            service.shutdown();
        }

    }
}
