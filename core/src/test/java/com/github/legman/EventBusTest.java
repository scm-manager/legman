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

package com.github.legman;

import org.assertj.guava.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Sebastian Sdorra
 */
class EventBusTest {

  private String weakReferenceTest;

  private String strongReferenceTest;

  @Test
  void testWeakReference() {
    EventBus bus = new EventBus();
    bus.register(new WeakListener());
    Assertions.assertThat(bus.handlersByType).hasSize(1);

    System.gc();

    bus.post("event");
    assertThat(weakReferenceTest).isNull();
    Assertions.assertThat(bus.handlersByType).isEmpty();
  }

  @Test
  void testStrongReference() {
    EventBus bus = new EventBus();
    bus.register(new StrongListener());
    System.gc();
    bus.post("event");
    assertThat(strongReferenceTest).isEqualTo("event");
  }

  @Test
  void testSyncListener() {
    EventBus bus = new EventBus();
    SyncListener listener = new SyncListener();
    bus.register(listener);
    bus.post("event");
    assertThat(listener.event).isEqualTo("event");
  }

  @Test
  void testAsyncListener() throws InterruptedException {
    EventBus bus = new EventBus();
    AsyncListener listener = new AsyncListener();
    bus.register(listener);
    bus.post("event");
    assertThat(listener.event).isNull();
    Thread.sleep(500L);
    assertThat(listener.event).isEqualTo("event");
  }

  @Test
  void testDeadEvent() {
    EventBus bus = new EventBus();
    SyncListener listener = new SyncListener();
    bus.register(listener);
    DeadEventListener deadEventListener = new DeadEventListener();
    bus.register(deadEventListener);
    bus.post(new Integer(12));
    assertThat(deadEventListener.event).isNotNull();
  }

  @Test
  void testRuntimeException() {
    EventBus bus = new EventBus();
    bus.register(new RuntimeExceptionListener());
    assertThrows(IllegalStateException.class, () -> bus.post("event"));
  }

  @Test
  void testCheckedException() {
    EventBus bus = new EventBus();
    bus.register(new CheckedExceptionListener());
    String event = "event";
    Object failedEvent = null;
    try {
      bus.post(event);
    } catch (EventBusException ex) {
      failedEvent = ex.getEvent();
    }
    assertThat(event).isEqualTo(failedEvent);
  }

  @Test
  void testAsyncException() {
    EventBus bus = new EventBus();
    bus.register(new AsyncCheckedExceptionListener());
    bus.post("event");
  }

  @Test
  void testThreadName() throws InterruptedException {
    EventBus bus = new EventBus("hansolo");
    ThreadNameTestListener listener = new ThreadNameTestListener();
    bus.register(listener);
    bus.post("event");
    Thread.sleep(200L);
    assertThat(listener.threadName).startsWith("hansolo-");
  }

  @Test
  void testCleanupWeakReferences() {
    EventBus bus = new EventBus();
    bus.register(new WeakListener());
    Assertions.assertThat(bus.handlersByType).hasSize(1);
    System.gc();
    bus.cleanupWeakReferences();
    Assertions.assertThat(bus.handlersByType).isEmpty();
  }

  @Test
  void testEmptyEventBus() {
    EventBus bus = new EventBus();
    bus.post("event");
  }

  @Test
  void testShutdown() throws InterruptedException {
    EventBus bus = new EventBus();
    AsyncListener listener = new AsyncListener();
    bus.register(listener);
    bus.shutdown();

    bus.post("event");

    assertThat(listener.event).isNull();
    Thread.sleep(500L);
    assertThat(listener.event).isNull();
  }

  /**
   * Listener classes
   */

  private static class ThreadNameTestListener {

    private String threadName;

    @Subscribe
    public void handleEvent(String event) {
      threadName = Thread.currentThread().getName();
    }
  }

  private static class AsyncCheckedExceptionListener {

    @Subscribe
    public void handleEvent(String event) throws IOException {
      throw new IOException();
    }
  }

  private static class RuntimeExceptionListener {

    @Subscribe(async = false)
    public void handleEvent(String event) {
      throw new IllegalStateException();
    }
  }

  private static class CheckedExceptionListener {

    @Subscribe(async = false)
    public void handleEvent(String event) throws IOException {
      throw new IOException();
    }
  }

  private static class DeadEventListener {

    private DeadEvent event;

    @Subscribe(async = false)
    public void handleEvent(DeadEvent event) {
      this.event = event;
    }
  }

  private static class AsyncListener {

    private String event;

    @Subscribe
    public void handleEvent(String event) {
      this.event = event;
    }
  }

  private static class SyncListener {

    private String event;

    @Subscribe(async = false)
    public void handleEvent(String event) {
      this.event = event;
    }
  }

  private class StrongListener {

    @Subscribe(async = false, referenceType = ReferenceType.STRONG)
    public void handleEvent(String event) {
      strongReferenceTest = event;
    }
  }

  private class WeakListener {

    @Subscribe(async = false)
    public void handleEvent(String event) {
      weakReferenceTest = event;
    }
  }

}
