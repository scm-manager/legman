# Guice Integration

The Guice support module can be used to register every class which is instanciated by Guice to the event bus.

## Installation

```xml
<dependency>
  <groupId>com.cloudogu.legman.support</groupId>
  <artifactId>guice</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Usage

```java
EventBus eventBus = new EventBus();
Injector injector = Guice.createInjector(
  new LegmanModule(eventBus)
  // other modules
);
```
