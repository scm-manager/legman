# Legman

> Legman is a fork of the [Guava Eventbus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained).

## Differences to Guava

* weak references by default
* synchronous and asynchronous dispatching in same EventBus
* all options are controlled by attributes of the subscribe annotation
* exceptions are propagated to caller (works only with synchronous dispatching)


## Maven

```xml
<dependencies>
    
  <dependency>
    <groupId>com.github.legman</groupId>
    <artifactId>core</artifactId>
    <version>1.0.0</version>
  </dependency>

</dependencies>

<repositories>

  <repository>
    <id>maven.scm-manager.org</id>
    <name>scm-manager release repository</name>
    <url>http://maven.scm-manager.org/nexus/content/groups/public</url>
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