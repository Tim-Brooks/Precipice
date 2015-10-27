# Release Notes

## 0.4.0

### Canceling for default PrecipiceFuture implementation.
- Right now does not cancel computation. But sets the result. And avoids first level metrics update.

### ActionMetrics updates
- Added tracking of latency for actions. The tracking is currently for the lifetime of the service. There is no
rolling latency measure.
- Added total metric counts to default action metrics.
- The snapshot function returns both these new features.

## 0.5.0

### ActionMetrics updates
- Add methods on DefaultActionMetrics to access metric counters.

### Latency Metrics Improvements
- Latency metrics are now captured in a specialized class.
- Latency is now partitioned by result - success, error, or timeout.