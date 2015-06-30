# Shotgun

This section is still being written.

## Rationale

This section is still being written.

## Usage

### Basic

A shotgun can by passing a map of CompletionService to context to the constructor. A shotgun only supports CompletionService, as the shotgun must pass single promise to all the services running an action. That way, the promise is complete when the first service has finished the action.

```java
Map<CompletionService, Map<String, String>> serviceToContext = new HashMap<>();
Shotgun shotgun = new Shotgun(serviceToContext, 2);
```