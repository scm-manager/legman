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

import com.github.legman.internal.ServiceLocator;

/**
 * Singleton factory for {@link EventBus} instances. The {@link EventBus} 
 * instances are stored by a {@link EventBusRegistryProvider}. The default 
 * implementation of the provider uses a {@link java.util.Map}. The 
 * implementation can be changed with service locator pattern.
 *
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public final class EventBusRegistry
{
 
  private static final EventBusRegistryProvider provider = 
    ServiceLocator.locate(
      EventBusRegistryProvider.class, DefaultEventBusRegistryProvider.class
    );
  
  public static EventBus getEventBus(){
    return getEventBus(EventBus.DEFAULT_NAME);
  }
  
  public static EventBus getEventBus(String name){    
    return provider.getEventBus(name);
  }
  
}
