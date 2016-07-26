package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.metrics.counts.Counters;
import net.uncontended.precipice.metrics.counts.PartitionedCount;
import net.uncontended.precipice.metrics.counts.RollingCounts;

public class RollingCountsBuilder<T extends Enum<T>> extends RollingBuilder<PartitionedCount<T>, RollingCounts<T>> {

    private final Class<T> clazz;

    public RollingCountsBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public RollingCounts<T> build() {
        if (allocator == null) {
            allocator = Counters.longAdder(clazz);
        }

        RollingMetrics<PartitionedCount<T>> rollingMetrics = buildRollingMetrics();
        return new RollingCounts<T>(rollingMetrics);
    }
}
