/*
 * Copyright (C) 2007 The Guava Authors and Sebastian Sdorra
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
import com.github.legman.Plugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Legman plugin to collect metrics about the event bus with Micrometer.
 *
 * @since 2.0.0
 */
public final class MicrometerPlugin implements Plugin {

  private final MeterRegistry registry;
  private final List<Tag> invocationTags = new ArrayList<>();
  private final List<Tag> executorTags = new ArrayList<>();

  /**
   * Creates a new instance of the {@link MicrometerPlugin}.
   *
   * @param registry registry which used to collect metrics
   */
  public MicrometerPlugin(MeterRegistry registry) {
    this.registry = registry;
  }

  /**
   * Extra tags which are added to the invocation metrics.
   *
   * @param tags list of extra tags
   * @return {@code this}
   */
  public MicrometerPlugin withInvocationTags(Tag... tags) {
    invocationTags.addAll(Arrays.asList(tags));
    return this;
  }

  /**
   * Extra tag which are added to the in executor service metrics.
   *
   * @param tags list of extra tags
   * @return {@code this}
   */
  public MicrometerPlugin withExecutorTags(Tag... tags) {
    executorTags.addAll(Arrays.asList(tags));
    return this;
  }

  @Override
  public void apply(EventBus.Builder builder) {
    builder.withInvocationInterceptors(new MicrometerInvocationInterceptor(registry, invocationTags))
           .withExecutorDecoratorFactories(new MicrometerExecutorDecoratorFactory(registry, executorTags));
  }
}
