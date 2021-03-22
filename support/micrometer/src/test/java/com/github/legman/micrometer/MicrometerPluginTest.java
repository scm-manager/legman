package com.github.legman.micrometer;


import com.github.legman.EventBus;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerPluginTest extends MicrometerTestBase {

  @Test
  void shouldCollectLegmanMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    eventBus = EventBus.builder()
            .withPlugins(new MicrometerPlugin(registry))
            .build();

    sendEventAndWait();

    List<String> metrics = registry.getMeters()
            .stream()
            .map(m -> m.getId().getName())
            .collect(Collectors.toList());

    assertThat(metrics).contains("legman.invocation", "executor.completed");
  }

  @Test
  void shouldAppendExecutorTags() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    eventBus = EventBus.builder()
            .withPlugins(new MicrometerPlugin(registry).withExecutorTags(Tag.of("spaceship", "HeartOfGold")))
            .build();

    assertThat(registry.get("executor.pool.size").gauge().getId().getTag("spaceship")).isEqualTo("HeartOfGold");
  }

  @Test
  void shouldAppendInvocationTags() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    eventBus = EventBus.builder()
            .withPlugins(new MicrometerPlugin(registry).withInvocationTags(Tag.of("spaceship", "RazorCrest")))
            .build();

    sendEventAndWait();

    assertThat(registry.get("legman.invocation").timer().getId().getTag("spaceship")).isEqualTo("RazorCrest");
  }

}
