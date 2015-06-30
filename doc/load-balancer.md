# Load Balancer

Out of the box, Precipice supports in-process load balancers. load balancer are a group of services composed together. When an action is sent to a load balancer, that action is forward to one of that load balancer's services. If that service immediately rejects the action, the action is attempted on the next service. If the action continues to be rejected, the action is sent to each service until either the action is accepted or the services are exhausted.

## Rationale

The goal of a load balancer is to improve the resiliency of your application. If one of your critical services is not responding, your program can continue if an alternative service is still available. Additionally, by spreading the actions across the services, the load is distributed, decreasing the probability of overloading one of your services.

Because the load balancer is in-process, a rejected action can immediately be attempted on another service.

## Usage

### Basic

A load balancer can be created using the LoadBalancers class. You pass a map of services and contexts and received a load balancer pattern that will use those services.

The context is necessary to contain any metadata associated with a specific service. 

```java
String serviceName1 = "Identity Service1";
String serviceName2 = "Identity Service2";
int poolSize = 5;
int concurrencyLevel = 100;
MultiService service1 = Services.defaultService(serviceName1, poolSize, concurrencyLevel);
Map<String, String> context1 = new HashMap<>();
context1.put("address", "127.0.0.1");
context1.put("port", "6001");
MultiService service2 = Services.defaultService(serviceName2, poolSize, concurrencyLevel);
Map<String, String> context2 = new HashMap<>();
context2.put("address", "127.0.0.1");
context2.put("port", "6002");

Map<MultiService, Map<String, String>> serviceToContext = new HashMap<>();
serviceToContext.put(service1, context1);
serviceToContext.put(service2, context2);

MultiPattern<Map<String, String>> balancer = LoadBalancers.newRoundRobin(serviceToContext);

ResilientFuture<String> f = balancer.submit(new ResilientPatternAction<String, Map<String, String>>() {
    @Override
    public String run(Map<String, String> context) throws Exception {
return context.get("port");
    }
}, 100L);

try {
    // Should return the port (6001 or 6002) of the service to which the action was sent
    f.get();
} catch (ExecutionException e) {
    e.getCause().printStackTrace();
}
```

### Specialized Load Balancers

Similar, to services there are three main interfaces for patterns. In the example above, I create a MultiPattern which is a superset of the three. However, specialized instances can be created.

```java
SubmissionPattern<Map<String, String>> submission = LoadBalancers.submittingRoundRobin(serviceToContext);
ResilientFuture<String> f = submission.submit(new ImplementedPatternAction(), 100L);

CompletionPattern<Map<String, String>> completion = LoadBalancers.completingRoundRobin(serviceToContext);
DefaultResilientPromise<String> p = new DefaultResilientPromise<>();
completion.submitAndComplete(new ImplementedPatternAction(), p, 100L);

RunPattern<Map<String, String>> run = LoadBalancers.runRoundRobin(serviceToContext);
try {
    run.run(new ImplementedPatternAction());
} catch (Exception e) {
    e.printStackTrace();
}
```

### Balancing Strategy

The default strategy for distributing actions is [round robin](https://en.wikipedia.org/wiki/Round-robin_DNS).
Different strategies can be produced by implementing the LoadBalancerStrategy interface.

```java
LoadBalancerStrategy strategy = new LoadBalancerStrategy() {
    @Override
    public int nextExecutorIndex() {
        // Implement your strategy
        return 0;
    }
};

MultiPattern<Map<String, String>> balancerWithUserStrategy = new LoadBalancer<>(serviceToContext, strategy);
```

### Shared Threadpool

One of the trade-offs with using the examples listed above to create a LoadBalancer, is that each service will have its own threadpool.

This might be what you want as this isolates each service. With isolated threadpools, a slow service will not consume the threads that another service would like to use.

The downside is that isolated threadpools increases the number of system threads your application requires. Depending on your use case and how aggressive your circuit breaker and timeouts are configured, you may want your load balancer to share a thread pool.

In the example below, you provide the contexts for your services and the services are created and put behind a load balancer that is sharing a threadpool.

The name is shared between services in this case, because the primary purpose of a service's name in this version of Precipice is to name the OS threads. As the threads are shared, the name is the same.

```java
List<Map<String, String>> contexts = new ArrayList<>();
contexts.addAll(serviceToContext.values());
MultiPattern<Map<String, String>> balancer = LoadBalancers.newRoundRobinWithSharedPool(contexts,
        "Identity Service", poolSize, concurrencyLevel);
```