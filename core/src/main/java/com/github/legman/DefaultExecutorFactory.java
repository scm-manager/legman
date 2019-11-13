package com.github.legman;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of {@link ExecutorFactory}.
 * Can be overwritten by defining a service for the java 6 {@link java.util.ServiceLoader} mechanism.
 *
 * @author Sebastian Sdorra
 * @since 1.1.0
 */
public class DefaultExecutorFactory implements ExecutorFactory {
  @Override
  public Executor create(String identifier) {
    return Executors.newFixedThreadPool(4, createThreadFactory(identifier));
  }

  private ThreadFactory createThreadFactory(String identifier) {
    return new ThreadFactoryBuilder()
            .setNameFormat(identifier.concat("-%s"))
            .build();
  }
}
