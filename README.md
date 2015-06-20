# Beehive

Beehive is a library to manage access to application services. Services are essentially targets of actions that can fail. Beehive provides a group of tools that can be composed together depending on your Service's needs: metrics, circuit breakers, in process load balancers, patterns, and more.

Actions can be submitted in either a synchronous or asynchronous manner. Beehive was designed with both performance and concurrency in mind.

## Version

This library has not yet hit alpha. It is used in production at Staples SparX. However, the API may still change.

In particular: the project will likely be moved from Lein to Gradle. It will be split so that the Java component can be used without Clojure.

## Usage

The basis of Beehive is the ServiceExecutor interface. A default implementation can be created from the static methods on the Service class.

```java
String name = "Identity Service";
int poolSize = 10;
int concurrencyLevel = 1000;
ServiceExecutor service = Service.defaultService(name, poolSize, concurrencyLevel);
```

In order can either submit or perform an action on this service. A submitted action is ran on the threadpool associated with the service. A performed action is completed on the thread calling performAction.

```java
ResilientAction<Integer> action = new ResilientAction<> {
    @Override
    public String run() throws Exception {
        URL obj = new URL("identity-service-url");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        return con.getResponseCode();
    }
}

ResilientPromise<Integer> promise = service.performAction(action);
Integer result = promise.awaitResult();
// Or:
long timeoutInMillis = 100;
ResilientFuture<Integer> future = service.submitAction(action, timeoutInMillis);
Integer result = future.get();
```

The default service includes both metrics and a load balancer. Actions that return successfully will be tallied as successes. Thrown exceptions will be tallied as errors. And a timeout will be tallied as a timeout.

There are two main mechanisms of back pressure to protect the service.

First, the concurrencyLevel is the maximum amount of uncompleted actions that a service can be working on at one time. Both performed and submitted actions add to this restriction.

Second, if a certain threshold is passed for failures (timeouts + errors) the circuit will open for a configurable time. Actions will be rejected while the circuit is open.

All components of the library are designed to be composable. If you would like to provide your own circuit breaker implementation or metrics implementation you are free to do so. The default metrics implementation is designed to be performant, avoid unnecessary allocations, and be lock free. It should be sufficient for most use cases.

Here are some common options for creating a service:
```java
String name = "Identity Service";
int poolSize = 10;
int concurrencyLevel = 1000;

// This creates a no op circuit breaker. The circuit will never open based on failures.
// However, the circuit can still be opened and closed manually be calling 
// forceOpen or forceClosed.
ServiceExecutor service = Service.defaultServiceWithNoOpBreaker(name, poolSize, concurrencyLevel);

// Service with user provided metrics.
ActionMetrics metrics = new UserCreatedMetrics();
ServiceExecutor service = Service.defaultService(name, poolSize, concurrencyLevel, metrics);

// Service with user provided metrics and circuit breaker.
CircuitBreaker breaker = new UserProvidedBreaker();
ServiceExecutor service = Service.defaultService(name, poolSize, concurrencyLevel, metrics, breaker);
```

Often, you will want to use the default metrics and breaker. There are a number of configurations of which you might want to be aware.

```java
// Circuit Breaker

BreakerConfigBuilder configBuilder = BreakerConfigBuilder();
configBuilder.trailingPeriodMillis = 1000L;
configBuilder.failureThreshold = Long.MAX_VALUE;
configBuilder.failurePercentageThreshold = 50;
configBuilder.healthRefreshMillis = 500L;
configBuilder.backOffTimeMillis = 1000L;
CircuitBreaker config = configBuilder.build();

// Action Metrics

// The default implementation is a circular array composed of slots.
// Slots to track is the size of the array.
int slotsToTrack = 3600;

// This this the resolution for the array. It is combined with the TimeUnit to determined
// when to move to the next slot. So a resolution of 1 combined with a TimeUnit.SECONDS
// means that each array slot contains the data for one second. A resolution of 500 with a 
// TimeUnit.MILLISECONDS means that each array slot contains the data for 500
// milliseconds.
long resolution = 1;
TimeUnit slotUnit = TimeUnit.SECONDS;
ActionMetrics metrics = DefaultActionMetrics(slotsToTrack, resolution, slotUnit)
```

## License

Copyright © 2014 Tim Brooks

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.