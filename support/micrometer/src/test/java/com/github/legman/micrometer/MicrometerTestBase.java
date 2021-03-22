package com.github.legman.micrometer;

import com.github.legman.EventBus;
import com.github.legman.Subscribe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class MicrometerTestBase {

  protected EventBus eventBus;

  @AfterEach
  void shutdown() {
    if (eventBus != null) {
      eventBus.shutdown();
    }
  }


  void sendEventAndWait() {
    sendEventAndWait("one");
  }

  void sendEventAndWait(Object event) {
    Subscriber subscriber = new Subscriber();

    sendEvent(subscriber, event);

    await().atMost(1, TimeUnit.SECONDS)
            .until(() -> "one".equals(subscriber.event));
  }

  void sendEvent(Object event) {
    sendEvent(new Subscriber(), event);
  }

  void sendEvent(Subscriber subscriber, Object event) {
    eventBus.register(subscriber);
    eventBus.post(event);
  }

  public static class Subscriber {

    private String event;

    @Subscribe
    public void handleEvent(String event) {
      this.event = event;
    }

    @Subscribe(async = false)
    public void handleEvent(Integer integer) {
      if (integer % 2 != 0) {
        throw new IllegalStateException("failed");
      }
    }

  }

}
