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
