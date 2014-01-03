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



package com.github.legman.test;

//~--- non-JDK imports --------------------------------------------------------

import com.github.legman.EventBus;
import com.github.legman.EventBusRegistryProvider;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sebastian Sdorra
 * @since 1.1.0
 */
public class ThreadLocalEventBusRegistryProvider implements EventBusRegistryProvider
{

  /** Field description */
  private static final ThreadLocal<Map<String, EventBus>> store =
    new ThreadLocal<Map<String, EventBus>>();

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   */
  static void release()
  {
    System.out.println("release");
    store.remove();
  }

  /**
   * Method description
   *
   */
  static void initialize()
  {
    System.out.println("initialize");
    store.set(new HashMap<String, EventBus>());
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param name
   *
   * @return
   */
  @Override
  public EventBus getEventBus(String name)
  {
    Map<String, EventBus> map = store.get();

    if (map == null)
    {
      throw new IllegalStateException(
        "thread local variable is not initialized, "
        + "you have to define the EventBusRule.");
    }

    EventBus eventBus = map.get(name);

    if (eventBus == null)
    {
      eventBus = new EventBus(name);
      map.put(name, eventBus);
    }

    return eventBus;
  }
}
