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

package com.github.legman;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.lang.ref.WeakReference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
  private final boolean asnyc;
  
  
  private final EventBus eventBus;

  /**
   * Creates a new EventHandler to wrap {@code method} on @{code target}.
   *
   * @param target  object to which the method applies.
   * @param method  handler method.
   */
  EventHandler(EventBus eventBus, Object target, Method method, ReferenceType referenceType, boolean asnyc) {
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
      this.targetReference = new WeakReference<Object>(target);
    }
    this.method = method;
    this.asnyc = asnyc;
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
  public void handleEvent(Object event) throws InvocationTargetException {
    checkNotNull(event);
    Object t = target;
    if (t == null){
      t = targetReference.get();
    }
    if ( t != null ){
      try {
        method.invoke(t, new Object[] { event });
      } catch (IllegalArgumentException e) {
        throw new Error("Method rejected target/argument: " + event, e);
      } catch (IllegalAccessException e) {
        throw new Error("Method became inaccessible: " + event, e);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof Error) {
          throw (Error) e.getCause();
        }
        throw e;
      }
    } else {
      eventBus.removeEventHandler(this);
    }
  }
    

  public boolean isAsnyc() {
    return asnyc;
  }


  @Override public boolean equals(@Nullable Object obj) {
    if (obj instanceof EventHandler) {
      EventHandler that = (EventHandler) obj;
      // Use == so that different equal instances will still receive events.
      // We only guard against the case that the same object is registered
      // multiple times
      return target == that.target && targetReference == that.targetReference && method.equals(that.method) && asnyc == that.asnyc;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(target, targetReference, method, asnyc);
  }


  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .addValue(target)
            .addValue(method)
            .addValue(asnyc)
            .toString();
  }
}
