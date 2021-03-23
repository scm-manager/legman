# Shiro Integration

The Apache Shiro support module allows the usage of SecurityManager and Subject from Shiro even in asynchronous subscribers. 

## Installation

```xml
<dependency>
  <groupId>com.github.legman.support</groupId>
  <artifactId>shiro</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Usage

```java
EventBus eventbus = EventBus.builder()
                            .plugins(new ShiroPlugin())
                            .build();
```
