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

  /**
   * Creates the default {@link Executor} for asynchronous event processing.
   *
   * @param identifier eventbus identifier
   * @return default executor
   */
  static Executor create(String identifier) {
    return Executors.newFixedThreadPool(4, createThreadFactory(identifier));
  }

  private static ThreadFactory createThreadFactory(String identifier) {
    return new ThreadFactoryBuilder()
            .setNameFormat(identifier.concat("-%s"))
            .build();
  }
}
