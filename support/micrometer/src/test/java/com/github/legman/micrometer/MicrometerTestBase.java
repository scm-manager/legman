/*
 * Copyright (C) 2013 SCM-Manager Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
