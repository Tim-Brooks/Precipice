# Precipice

Precipice is a library that helps you moniter and manage the execution of tasks that can fail. This could be anything from a network request to an external system or specific procedures internal to your application.

## Design

The core abstraction of Precipice is the concept of a **guardrail**. If you would like to execute a task, you request permits from the guardrail. The decision about whether permits are available is determined based on user configurable metrics and backpressure mechanisms.

Specifically a guardrail has:

1. Metrics about the successes and failures of executions.
2. Metrics about attempts to acquire permits for execution that have been rejected.
3. Optional latency metrics about the results of exeuctions.
4. Zero or more back pressure mechanisms informed by these metrics.

There is also a Precipice interface that has a single method to return a guard rail. This allows the creation of specialized implementations. For example, the precipice-threadpool package includes an implementation that surrounds the usage of threadpool with a guardrail.

## Version

This library has not yet hit alpha. It is used in production at [Staples SparX](http://www.staples-sparx.com). However, the API may still change.

The latest release is available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7CPrecipice).

## Usage

### Creating a GuardRail

GuardRails can be created using the builder. A name, metrics for tracking results, and metrics for tracking rejections are required. Latency metrics and back pressure mechanisms are optional.

As a note, you must specify enumerated types to define possible results or rejections. There are a number of common options provided. Or you can implement your own.

```java
String name = "Identity Service";
GuardRailBuilder builder = new GuardRailBuilder<>();
builder.name(name);
builder.resultMetrics(new RollingCountMetrics<>(SimpleResult.class));
builder.rejectedMetrics(new RollingCountMetrics<>(Unrejectable.class));

GuardRail guardRail = builder.build();
```

### Using a GuardRail

The simplest way to use the GuardRail is by using on of the factories for completion contexts that is provided. These factories implement the logic for acquiring permits and the logic of releasing permits on completion of the context.

```java
GuardRail<SimpleResult, Unrejectable> guardRail = builder.build();
CompletionContext<SimpleResult, String> completable = Synchronous.acquirePermitsAndCompletable(guardRail, 1L);

try {
    URL url = new URL("http://www.google.com");
    URLConnection urlConnection = url.openConnection();
    completable.complete(SimpleResult.SUCCESS, readToString(urlConnection.getInputStream()));
  } catch (Exception ex) {
    completable.completeExceptionally(SimpleResult.ERROR, ex);
  }
```

### Using the CallService

The CallService is an provides a Precipice implementation that works out of the box. It takes a GuardRail parameterized with SimpleResult for the result type. It is called with Callables, similar to a ExecutorService. However, the Callables are executed on the calling thread.

The CallService will only execute the Callable if a permit is available from the GuardRail, otherwise will throw an RejectedException. CallService implements the Precipice interface, so you can call the guardRail() method to get its GuardRail. With the GuardRail you can interrogate metrics related to callable results, rejections, and latency.

```java
CallService<Unrejectable> callService = new CallService<>(guardRail);

try {
	Request req = callService.call(() -> client.submitHttpClient());
} catch (RejectedException e) {
	// Handle exception
}
```

### Specialized Implementations

The precipice-samples module provides examples of specialized implementions. These demonstrate how specific protocols or clients can be monitored and managed using a GuardRail.

Two examples are:
- Kafka producer service
- Http client service

Additionally, the precipice-threadpool module provides a production-ready implementation of a threadpool protected by a GuardRail.

## License

Copyright © 2014-2016 Tim Brooks

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.