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

/**
 * This exception is thrown if a check exception which could not be propagated
 * during synchronous dispatching.
 *
 * @author Sebastian Sdorra
 * @since 1.2.0
 */
public class EventBusException extends RuntimeException {

  @SuppressWarnings("java:S1948")
  private final Object event;

  /**
   * Constructs new {@link EventBusException}.
   *
   * @param event event, which could not be dispatched
   * @param message exception message
   * @param cause cause of the exception
   */
  public EventBusException(Object event, String message, Throwable cause) {
    super(message, cause);
    this.event = event;
  }

  /**
   * Returns the event, which could not be dispatched.
   *
   *
   * @return event
   */
  public Object getEvent() {
    return event;
  }
}
