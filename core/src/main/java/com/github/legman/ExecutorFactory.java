package com.github.legman;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The {@link ExecutorFactory} creates the default {@link Executor}.
 * If no other {@link Executor} is specified in the builder.
 *
 * @author Sebastian Sdorra
 * @since 1.1.0
 */
class ExecutorFactory {

  private ExecutorFactory() {
  }

  public static Executor create(String identifier) {
    return Executors.newFixedThreadPool(4, createThreadFactory(identifier));
  }

  private static ThreadFactory createThreadFactory(String identifier) {
    return new ThreadFactoryBuilder()
            .setNameFormat(identifier.concat("-%s"))
            .build();
  }
}
