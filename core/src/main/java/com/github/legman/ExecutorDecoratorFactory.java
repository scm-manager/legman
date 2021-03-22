/*
 * Copyright (C) 2013 Sebastian Sdorra
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

import java.util.concurrent.Executor;

/**
 * Factory which is able to create an decorator for the {@link Executor}
 * which is used by the {@link EventBus} for asynchronous dispatching of events.
 *
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public interface ExecutorDecoratorFactory {

  /**
   * Returns the decorated {@link Executor}.
   *
   * @param executor executor to decorate
   * @return decorated {@link Executor}
   */
  Executor decorate(Executor executor);
}
