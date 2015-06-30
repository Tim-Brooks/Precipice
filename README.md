# Precipice

Precipice is a library that provides the building blocks to manage access to services that your application utilizes. A service can be a number of different things. A service could be a in-process logger performing file IO. Or a service could be something remote, like a different HTTP server. 

Generally, a service is an self-contained unit of functionality that can fail. When one of your services fails, Precipice provides you the tools to handle and isolate that failure within your application.

## Design

The concept of a **service** is the basic abstraction backing Precipice. A Precipice service has two primary features:

1. Metrics about the success and failures of actions on that service.
2. A mechanism to provide backpressure when the service is overloaded or failing.

Precipice does not mandate any specific execution model. There are a variety of interfaces that allow you to decide what makes most sense for your use case. It is even possible for an action run on a Precipice service to be executed synchronously (on the calling thread).

All of the default Precipice services are composed of four components: an execution model (often a threadpool), a semaphore, metrics, and a circuit breaker. The semaphore prevents too many pending actions. The metrics provide feedback for executed actions. And the circuit breaker rejects actions when the service is failing. The circuit breaker can also be opened manually.

All components are designed to be composable. This allows you to create a service that specifically meets your needs. Two examples might be:

1. Use one of the default services and circuit breakers, but provide a different metrics class to capture additional information about service actions.
2. Use the default metrics and circuit breaker, but implement a different execution model using something like [Quasar](https://github.com/puniverse/quasar).

One tactic to increase resiliency of your services is to combine redundant services into **patterns**. A Precipice pattern is multiple services combined together with a strategy for how to handle new actions.

There are two patterns included:

1. [Load balancer](https://github.com/tbrooks8/Precipice/blob/master/doc/load-balancer.md) - The load balancer will distribute actions to different services, preferring services that are not failing.
2. [Shotgun](https://github.com/tbrooks8/Precipice/blob/master/doc/shotgun.md) - The shotgun will send an action to multiple services. The first service to respond will be the result of the action.

## Version

This library has not yet hit alpha. It is used in production at [Staples SparX](http://www.staples-sparx.com). However, the API may still change.

The latest release is available on Maven Central:

```xml
<dependency>
  <groupId>net.uncontended</groupId>
  <artifactId>Precipice</artifactId>
  <version>0.1.1</version>
</dependency>
```

## Usage

Precipice provides three service interfaces: [CompletionService](https://github.com/tbrooks8/Precipice/blob/master/src/main/java/net/uncontended/precipice/CompletionService.java), [SubmissionService](https://github.com/tbrooks8/Precipice/blob/master/src/main/java/net/uncontended/precipice/SubmissionService.java), and [RunService](https://github.com/tbrooks8/Precipice/blob/master/src/main/java/net/uncontended/precipice/RunService.java). Each provides a different model concurrency model. SubmissionService is the style most similar to the Java ExecutorService. You provide an action to be performed asynchronously and receive a future representing that asynchronous computation.

Default implementations can be created from the static methods on the [Services](https://github.com/tbrooks8/Precipice/blob/master/src/main/java/net/uncontended/precipice/Services.java) class.

```java
String name = "Identity Service";
int poolSize = 10;
int concurrencyLevel = 1000;
SubmissionService service = Services.submissionService(name, poolSize, concurrencyLevel);
```

An action submitted to this service is executed in the background on a threadpool associated with the service.

```java
ResilientAction<Integer> action = new ResilientAction<> {
    @Override
    public String run() throws Exception {
        URL obj = new URL("identity-service-url");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        return con.getResponseCode();
    }
}

long timeoutInMillis = 100;
ResilientFuture<Integer> future = service.submitAction(action, timeoutInMillis);
Integer result = future.get();
```

The default service in this example provides both metrics and a load balancer. Actions that return successfully will be tallied as successes. Thrown exceptions will be tallied as errors. And a timeout will be tallied as a timeout.

There are two main mechanisms of back pressure to protect the service.

1. The concurrencyLevel is the maximum amount of uncompleted actions that a service can be working on at one time.
2. If a certain threshold is passed for failures (timeouts + errors) the circuit will open for a configurable time. Actions will be rejected while the circuit is open.

All components of the library are designed to be composable. If you would like to provide your own circuit breaker implementation or metrics implementation you are free to do so. The default metrics implementation is designed to be performant, avoid unnecessary allocations, and be lock free. It should be sufficient for most use cases.

Here are some common options for creating a service:
```java
String name = "Identity Service";
int poolSize = 10;
int concurrencyLevel = 1000;

// This creates a no op circuit breaker. The circuit will never open based on failures.
// However, the circuit can still be opened and closed manually be calling 
// forceOpen or forceClosed.
SubmissionService service = Services.submissionServiceWithNoOpBreaker(name, poolSize, concurrencyLevel);

// Service with user provided metrics.
ActionMetrics metrics = new UserCreatedMetrics();
SubmissionService service = Services.submissionService(name, poolSize, concurrencyLevel, metrics);

// Service with user provided metrics and circuit breaker.
CircuitBreaker breaker = new UserProvidedBreaker();
SubmissionService service = Services.submissionService(name, poolSize, concurrencyLevel, metrics, breaker);
```

Most of the time you will want to use the default metrics and breaker. There are a number of configurations of which you might want to be aware.

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

// This this the resolution for the array. It is combined with the TimeUnit to determine
// when to move to the next slot. So a resolution of 1 combined with TimeUnit.SECONDS
// means that each array slot contains the data for one second. A resolution of 500 with 
// TimeUnit.MILLISECONDS means that each array slot contains the data for 500
// milliseconds.
long resolution = 1;
TimeUnit slotUnit = TimeUnit.SECONDS;
ActionMetrics metrics = DefaultActionMetrics(slotsToTrack, resolution, slotUnit)
```

Further examples of service usage can be found in the [example](https://github.com/tbrooks8/Precipice/tree/master/src/test/java/net/uncontended/precipice/example) package.

In that package, there are examples for both the RunService interface and the CompletionService. The RunService executes actions on the calling thread. The CompletionService executes actions asynchronously in the background. However, the submitAndComplete method is void opposed to returning a future. To receive the result, the user supplies a promise that will be completed.

## Roadmap

1. Revisit all names. As we near a finalize API, any method or class name that can be improved will be improved. The final public API should be determined by the end of August.
2. Continue to add documentation. This includes Javadoc, examples, and discussion of architecture.
3. Improved metrics. Currently metrics are pretty basic and only include counts. Some sense of latency seems to make sense. Additionally, it may make sense for patterns to have their own metrics.
4. Ensure some amount of compatibility with [reactive streams](http://www.reactive-streams.org/).
5. Improved performance of submitting to the default services.

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