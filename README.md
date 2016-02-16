# Precipice

Precipice is a library that provides the building blocks to manage access to services that your application utilizes. A service can be a number of different things. A service could be a in-process logger performing file IO. Or a service could be something remote, like a different HTTP server. 

Generally, a service is an self-contained unit of functionality that can fail. When one of your services fails, Precipice provides you the tools to handle and isolate that failure within your application.

## Design

The abstraction of Precipice is the concept of a **guardrail**. A guardrail protects the portion of your application that may fail upon execution. There is a single implementation of this class that can be used for numerous cases. A GuardRail has two primary features:

1. Metrics about the successes and failures of the execution.
2. A mechanism to provide backpressure when the execution is failing.

Using the a GuardRail to protect your application is quite straightforward. You build one with the metrics that you are interested in collecting and the backpressure mechanisms you are interested in using. Some examples would be semaphores, circuit breakers, or rate limiters.

When you are interested in executing the protected area of code, you acquire a permit from the guardrail. When the action is complete, you release the permit.

## Version

This library has not yet hit alpha. It is used in production at [Staples SparX](http://www.staples-sparx.com). However, the API may still change.

The latest release is available on Maven Central:

```xml
<dependency>
  <groupId>net.uncontended</groupId>
  <artifactId>Precipice</artifactId>
  <version>0.6.0-SNAPSHOT</version>
</dependency>
```

## Usage

GuardRails can be created using the builder. A name, metrics for tracking results, and metrics for tracking rejections are required. Latency metrics and back pressure mechanisms are optional. You must specific types for results and rejections. Both of these types must be enumerations. The result type must implement an interface that indicates if it is a success or failure.

```java
String name = "Identity Service";
GuardRailBuilder builder = new GuardRailBuilder<>();
builder.name(name);
builder.resultMetrics(new RollingCountMetrics<>(Status.class));
builder.rejectedMetrics(new RollingCountMetrics<>(Rejected.class));

GuardRail guardRail = builder.build();
```

An action can be performed as follows.

```java
long nanoStartTime = System.nanoTime();
Rejected rejectedReason = guardRail.acquirePermits(1, nanoStartTime);
if (rejectedReason == null) {
	try {
		Request req = client.submitHttpClient();
		long endNanoTime = System.nanoTime();
		guardRail.releasePermits(1, Status.SUCCESS, nanoStartTime, endNanoTime);
	} catch (Exception e) {
		long endNanoTime = System.nanoTime();
		guardRail.releasePermits(1, Status.ERROR, nanoStartTime, endNanoTime);
	}
}

```

If you are interested in integrating your work into a class wrapping this related work there is an **Precipice** interface. This interface has a single method to return the Precipice's guardrail.

There is an generic CallService for wrapping Callables with the Guardrail protections. This will wire up all acquiring and releasing of permits for you.

```java
CallService<RejectedType> callService = new CallService<>(guardRail);

try {
	Request req = callService.call(() -> client.submitHttpClient());
} catch (RejectedException e) {
	// Handle exception
} 
```

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