# Micrometer Integration

The Micrometer support module collect metrics about invocations and usage of the underlying executor using micrometer.

## Installation

```xml
<dependency>
  <groupId>com.github.legman.support</groupId>
  <artifactId>micrometer</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Usage

```java
MeterRegistry registry = // create your micrometer registry
EventBus eventbus = EventBus.builder()
                            .plugins(new MicrometerPlugin(registry))
                            .build();
```
