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

## 0.5.1

### Latency Metrics
- Add latency metrics to ServiceProperties
- Remove the implicit initialization of LatencyMetrics in AbstractService. The metrics now must be passed in the
constructor.

## 0.5.2

### Latency Metrics
- Make latency metrics capture intervals.

## 0.6.2

### Latency and Count Metrics
- The GuardRail will now pass the permit count to the metrics.
- A number of naming conventions have been changes with regard to metrics.
- There are now some metric classes that increment and others that add the total permit count.