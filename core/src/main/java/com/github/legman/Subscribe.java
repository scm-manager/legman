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

//~--- JDK imports ------------------------------------------------------------

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler, as used by
 * {@link AnnotatedHandlerFinder} and {@link EventBus}.
 *
 * <p>The type of event will be indicated by the method's first (and only)
 * parameter.  If this annotation is applied to methods with zero parameters,
 * or more than one parameter, the object containing the method will not be able
 * to register for event delivery from the {@link EventBus}.
 *
 * @author Cliff Biffle
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
  /**
   * Returns true if the event should be handled asynchronous.
   * The default is asynchronous.
   *
   * @return true if the event should be handled asynchronous.
   */
  boolean async() default true;

  /**
   * Marks an event handling method as being thread-safe.  This attribute
   * indicates that EventBus may invoke the event handler simultaneously from
   * multiple threads.
   *
   * @return false if the annotated method is thread-safe
   */
  boolean allowConcurrentAccess() default false;

  /**
   * By default all references to message listeners are weak to eliminate the
   * risk of a memory leak. It is possible to use strong references.
   *
   * @return the reference type
   */
  ReferenceType referenceType() default ReferenceType.WEAK;
}
