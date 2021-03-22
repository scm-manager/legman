package com.github.legman.micrometer;

import com.github.legman.EventBus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MicrometerInvocationInterceptorTest extends MicrometerTestBase {

  @Test
  void shouldCollectMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    eventBus = EventBus.builder()
            .withIdentifier("ateb")
            .withInvocationInterceptors(new MicrometerInvocationInterceptor(registry, Collections.emptyList()))
            .build();

    sendEventAndWait();

    Timer timer = registry.get("legman.invocation").timer();
    assertThat(timer.count()).isEqualTo(1L);

    Meter.Id id = timer.getId();
    assertThat(id.getTag("bus")).isEqualTo("ateb");
    assertThat(id.getTag("event")).isEqualTo("java.lang.String");
    assertThat(id.getTag("target")).isEqualTo("com.github.legman.micrometer.MicrometerTestBase$Subscriber#handleEvent");
    assertThat(id.getTag("exception")).isEqualTo("none");
    assertThat(id.getTag("async")).isEqualTo("true");
  }

  @Test
  void shouldCollectMetricsForSynchronousEvents() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    eventBus = EventBus.builder()
            .withInvocationInterceptors(new MicrometerInvocationInterceptor(registry, Collections.emptyList()))
            .build();

    sendEvent(42);

    Timer timer = registry.get("legman.invocation").timer();
    assertThat(timer.count()).isEqualTo(1L);

    Meter.Id id = timer.getId();
    assertThat(id.getTag("bus")).isEqualTo("default");
    assertThat(id.getTag("event")).isEqualTo("java.lang.Integer");
    assertThat(id.getTag("async")).isEqualTo("false");
    assertThat(id.getTag("exception")).isEqualTo("none");
  }

  @Test
  void shouldCreateExceptionTag() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    eventBus = EventBus.builder()
            .withInvocationInterceptors(new MicrometerInvocationInterceptor(registry, Collections.emptyList()))
            .build();

    assertThrows(IllegalStateException.class, () -> sendEvent(21));

    Meter.Id id = registry.get("legman.invocation").timer().getId();
    assertThat(id.getTag("exception")).isEqualTo("java.lang.IllegalStateException");
  }

}
