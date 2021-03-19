package com.github.legman.micrometer;

import com.github.legman.ExecutorDecoratorFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class MicrometerExecutorDecoratorFactory implements ExecutorDecoratorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MicrometerExecutorDecoratorFactory.class);

  private final MeterRegistry registry;
  private final String executorServiceName;
  private final String metricPrefix;
  private final Iterable<Tag> tags;

  public MicrometerExecutorDecoratorFactory(MeterRegistry registry, String executorServiceName, String metricPrefix, Iterable<Tag> tags) {
    this.registry = registry;
    this.executorServiceName = executorServiceName;
    this.metricPrefix = metricPrefix;
    this.tags = tags;
  }

  @Override
  public Executor decorate(Executor executor) {
    if (executor instanceof ExecutorService) {
      LOG.trace("collect metrics for executor service");
      new ExecutorServiceMetrics(
        (ExecutorService)executor, executorServiceName, metricPrefix, tags
      ).bindTo(registry);
    } else {
      LOG.warn("could only collect metrics for instances of ExecutorService and not for {}", executor.getClass());
    }
    return executor;
  }
}
