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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.lang.ref.WeakReference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Wraps a single-argument 'handler' method on a specific object.
 *
 * <p>This class only verifies the suitability of the method and event type if
 * something fails.  Callers are expected to verify their uses of this class.
 *
 * <p>Two EventHandlers are equivalent when they refer to the same method on the
 * same object (not class).   This property is used to ensure that no handler
 * method is registered more than once.
 *
 * @author Cliff Biffle
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
class EventHandler {

  /** Object sporting the handler method. */
  private final Object target;

  private final WeakReference<Object> targetReference;

  /** Handler method. */
  private final Method method;

  /** Event should be handled asynchronous. */
  private final boolean async;

  private final EventBus eventBus;

  /**
   * Creates a new EventHandler to wrap {@code method} on @{code target}.
   *
   * @param eventBus bus which handles the event
   * @param target object to which the method applies.
   * @param method handler method.
   * @param referenceType type of the reference
   * @param async true if the event should be handled async
   */
  @SuppressWarnings("java:S3011")
  EventHandler(EventBus eventBus, Object target, Method method, ReferenceType referenceType, boolean async) {
    Preconditions.checkNotNull(eventBus, "eventbus cannot be null.");
    Preconditions.checkNotNull(target,
        "EventHandler target cannot be null.");
    Preconditions.checkNotNull(method, "EventHandler method cannot be null.");
    this.eventBus = eventBus;
    if ( referenceType == ReferenceType.STRONG ){
      this.target = target;
      this.targetReference = null;
    } else {
      this.target = null;
      this.targetReference = new WeakReference<>(target);
    }
    this.method = method;
    this.async = async;
    method.setAccessible(true);
  }

  /**
   * Invokes the wrapped handler method to handle {@code event}.
   *
   * @param event  event to handle
   * @throws InvocationTargetException  if the wrapped method throws any
   *     {@link Throwable} that is not an {@link Error} ({@code Error} instances are
   *     propagated as-is).
   */
  public void handleEvent(final Object event) throws InvocationTargetException {
    checkNotNull(event);
    Object t = getTarget();
    if ( t != null ){
      InvocationContext context = new InvocationContext(
        eventBus, method, t, event, async
      );
      context.proceed();
    } else {
      eventBus.removeEventHandler(this);
    }
  }

  /**
   * Returns target object or null if the event handler uses a weak reference
   * and the object is no longer available.
   *
   * @return target object
   *
   * @since 1.3.0
   */
  Object getTarget() {
    Object t = target;
    if (t == null){
      t = targetReference.get();
    }
    return t;
  }

  /**
   * Returns {@code true} if the event should be handled asynchronous.
   *
   * @return {@code true} if the event is handled async.
   */
  public boolean isAsync() {
    return async;
  }

  @Override public boolean equals(@Nullable Object obj) {
    if (obj instanceof EventHandler) {
      EventHandler that = (EventHandler) obj;
      // Use == so that different equal instances will still receive events.
      // We only guard against the case that the same object is registered
      // multiple times
      return target == that.target && targetReference == that.targetReference && method.equals(that.method) && async == that.async;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, targetReference, method, async);
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .addValue(target)
            .addValue(method)
            .addValue(async)
            .toString();
  }

  boolean hasToBeSynchronized() {
    return false;
  }
}
