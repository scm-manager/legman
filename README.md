# Legman

> Legman is a fork of the [Guava Eventbus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained).

## Differences to Guava

* weak references by default
* synchronous and asynchronous dispatching in same EventBus
* all options are controlled by attributes of the subscribe annotation
* exceptions are propagated to caller (works only with synchronous dispatching)

In addition, Legman comes with ready to use integrations:

* [Apache Shiro](support/shiro/README.md)
* [Google Guice](support/guice/README.md)
* [Micrometer](support/micrometer/README.md)

## Maven

```xml
<dependencies>
    
  <dependency>
    <groupId>com.github.legman</groupId>
    <artifactId>core</artifactId>
    <version>2.0.0</version>
  </dependency>

</dependencies>

<repositories>

  <repository>
    <id>packages.scm-manager.org</id>
    <name>scm-manager repository</name>
    <url>https://packages.scm-manager.org/repository/public/</url>
  </repository>

</repositories>
```

## Usage

```java
public class Event {

}

public class Listener {

  @Subscribe
  public void handleEvent(Event event){
    // handle event
  }

}

Listener listener = new Listener();

EventBus eventBus = new EventBus();
eventBus.register(listener);
eventBus.post(new Event());
```
