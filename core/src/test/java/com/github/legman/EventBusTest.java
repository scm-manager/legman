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

import com.google.common.base.Stopwatch;
import org.assertj.guava.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.synchronizedCollection;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

  @Test
  void testConcurrentExecution() {
    EventBus bus = new EventBus();
    ConcurrentListener concurrentListener = new ConcurrentListener();
    bus.register(concurrentListener);
    bus.post("event");
    bus.post("event");
    bus.post("event");
    assertThat(concurrentListener.concurrentAccessDetected).isTrue();
  }

  @Test
  void testNonConcurrentExecution() {
    EventBus bus = new EventBus();
    NonConcurrentListener concurrentListener = new NonConcurrentListener();
    bus.register(concurrentListener);
    bus.post("event");
    bus.post("event");
    bus.post("event");
    assertThat(concurrentListener.concurrentAccessDetected).isFalse();
  }

  @Test
  void shouldNotBeBlockedByLongRunningEventHandlers() throws InterruptedException {
    EventBus bus = new EventBus();
    LongRunningListener longRunningListener = new LongRunningListener();
    bus.register(longRunningListener);
    QuickListener quickListener = new QuickListener();
    bus.register(quickListener);

    bus.post("event");
    bus.post("event");
    bus.post("event");
    bus.post("event");
    bus.post("event");

    quickListener.awaitCountDown();
    assertThat(longRunningListener.currentCount()).isZero();
  }

  @Test
  @Disabled("for manual testing only")
  void shouldHandleMassiveEvents() {
    EventBus bus = new EventBus();
    Collection<RandomNonConcurrentListener> listener = IntStream.range(1, 7)
            .mapToObj(RandomNonConcurrentListener::new)
            .collect(Collectors.toList());
    listener.forEach(bus::register);

    AtomicLong maxPostTime = new AtomicLong(0);

    new Thread(() ->
            IntStream.range(0, 1000)
                    .peek(i -> {
                      try {
                        Thread.sleep((long) (200 * Math.random()));
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                    })
                    .forEach(event -> {
                      System.out.printf("posting event %s%n", event);
                      Stopwatch stopwatch = Stopwatch.createStarted();
                      bus.post(event);
                      long postTimeInMs = stopwatch.elapsed().get(ChronoUnit.NANOS) / 1000000;
                      System.out.printf("posting event %s took %sms%n", event, postTimeInMs);
                      maxPostTime.getAndAccumulate(postTimeInMs, Math::max);
                    })).start();

    await().atMost(1000, SECONDS)
            .untilAsserted(() -> {
              listener.forEach(
                      l -> assertThat(l.handledEventCount()).as("checking executed tasks of event handler #%s", l.nr).isEqualTo(1000)
              );
            });

    assertThat(maxPostTime.get()).isLessThan(10);
  }

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

  private static class ConcurrentListener {

    private final AtomicInteger currentAccessCount = new AtomicInteger(0);
    private boolean concurrentAccessDetected = false;

    @Subscribe(allowConcurrentAccess = true)
    public void handleEvent(String event) throws InterruptedException {
      if (currentAccessCount.getAndIncrement() > 0) {
        concurrentAccessDetected = true;
      }
      Thread.sleep(1000);
      currentAccessCount.decrementAndGet();
    }
  }

  private static class NonConcurrentListener {

    private final AtomicInteger currentAccessCount = new AtomicInteger(0);
    private boolean concurrentAccessDetected = false;

    @Subscribe(allowConcurrentAccess = false)
    public void handleEvent(String event) throws InterruptedException {
      if (currentAccessCount.getAndIncrement() > 0) {
        concurrentAccessDetected = true;
      }
      Thread.sleep(1000);
      currentAccessCount.decrementAndGet();
    }
  }

  private static class LongRunningListener {

    private AtomicInteger readyCount = new AtomicInteger(0);

    @Subscribe
    public void handleEvent(String event) {
      try {
        Thread.sleep(1000);
        readyCount.incrementAndGet();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    int currentCount() {
      return readyCount.get();
    }
  }

  private static class QuickListener {

    private final CountDownLatch countDownLatch = new CountDownLatch(5);

    @Subscribe
    public void handleEvent(String event) {
      System.out.println(countDownLatch.getCount());
      countDownLatch.countDown();
    }

    void awaitCountDown() throws InterruptedException {
      countDownLatch.await();
    }
  }

  private static class RandomNonConcurrentListener {

    private final Collection<Integer> handledEvents = synchronizedCollection(new HashSet<>(1000));
    private final int nr;

    public RandomNonConcurrentListener(int nr) {
      this.nr = nr;
    }

    @Subscribe
    public void handleEvent(Integer event) {
      try {
        System.out.printf("Running %s - %s%n", nr, event);
        Thread.sleep((long)(Math.random() * 1000 / nr));
        handledEvents.add(event);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private int handledEventCount() {
      return handledEvents.size();
    }
  }
}
