/*
 * Copyright (C) 2007 The Guava Authors and SCM-Manager Team
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a single-argument 'handler' method on a specific object, and ensures
 * that only one thread may enter the method at a time.
 *
 * <p>Beyond synchronization, this class behaves identically to
 * {@link EventHandler}.
 *
 * @author Cliff Biffle
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
final class SynchronizedEventHandler extends EventHandler {
  /**
   * Creates a new SynchronizedEventHandler to wrap {@code method} on
   * {@code target}.
   *
   * @param eventBus bus which handles the event
   * @param target object to which the method applies.
   * @param method handler method.
   * @param referenceType type of the reference
   * @param async true if the event should be handled async
   */
  public SynchronizedEventHandler(EventBus eventBus, Object target, Method method, ReferenceType referenceType, boolean async) {
    super(eventBus, target, method, referenceType, async);
  }

  @Override
  public void handleEvent(Object event) throws InvocationTargetException {
    // https://code.google.com/p/guava-libraries/issues/detail?id=1403
    synchronized (this) {
      super.handleEvent(event);
    }
  }
}
