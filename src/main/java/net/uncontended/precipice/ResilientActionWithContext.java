package net.uncontended.precipice;

/**
 * Created by timbrooks on 6/19/15.
 */
class ResilientActionWithContext<T, C> implements ResilientAction<T> {
    public C context;
    private final ResilientPatternAction<T, C> action;

    public ResilientActionWithContext(ResilientPatternAction<T, C> action) {
        this.action = action;
    }

    @Override
    public T run() throws Exception {
        return action.run(context);
    }
}
