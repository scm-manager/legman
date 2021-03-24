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
