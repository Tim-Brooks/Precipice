# Load Balancer

Out of the box, Precipice supports in-process load balancers. Load Balancers are a group of services composed
together. When an action is submitted or perform on a Load Balancer, that action is sent to one of the services. If
that service immediately rejects the action, the action is attempted on the next services. If the action continues to
be rejected, the action is tried to each service until the services are exhausted.

## Rationale

The point of a Load Balancer is to improve resiliency. If one of your services is not responding, your program can
continue if other services are still available. Additionally, by spreading the actions across the services, the load
is distributed, decreasing the chance of overloading your services.

Since the load balancer is in-process, a rejected action can immediately be attempted on another service.

## Usage

A load balancer can be created using the LoadBalancers class.

```java
Map<Service, Map<String, String>> serviceToContext = new HashMap<>();
ComposedService<C> balancer = LoadBalancers.newRoundRobin(serviceToContext);
```

The default strategy for distributing actions is [round robin](https://en.wikipedia.org/wiki/Round-robin_DNS).
Different strategies can be produced by implementing the LoadBalancerStrategy interface.

One of the trade-offs with using the strategies listed above to create a LoadBalancer, is that each service will have
its own threadpool. This might be ideal as this isolates each service. Long running actions will not block actions on
other services. However, it also require more OS threads.

A LoadBalancer with a shared threadpool might be preferred to decrease the amount of OS threads in your application.

```java
String name = "Load Balancer";
int concurrencyLevel = 1000;
int poolSize = 30;

List<C> contexts = new ArrayList<>();

ComposedService<C> balancer = newRoundRobinWithSharedPool(contexts, name, poolSize, concurrencyLevel);
```