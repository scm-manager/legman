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

import com.github.legman.ExecutorDecoratorFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Collect metrics about the executor which is used to process asynchronous events.
 *
 * @since 2.0.0
 */
public class MicrometerExecutorDecoratorFactory implements ExecutorDecoratorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MicrometerExecutorDecoratorFactory.class);

  private final MeterRegistry registry;
  private final Iterable<Tag> tags;

  MicrometerExecutorDecoratorFactory(MeterRegistry registry, Iterable<Tag> tags) {
    this.registry = registry;
    this.tags = tags;
  }

  @Override
  public Executor decorate(Executor executor) {
    if (executor instanceof ExecutorService) {
      LOG.trace("collect metrics for executor service");
      new ExecutorServiceMetrics(
        (ExecutorService)executor, "legman", tags
      ).bindTo(registry);
    } else {
      LOG.warn("could only collect metrics for instances of ExecutorService and not for {}", executor.getClass());
    }
    return executor;
  }
}
