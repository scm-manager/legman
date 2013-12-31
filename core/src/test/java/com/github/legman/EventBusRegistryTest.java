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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sebastian Sdorra
 */
public class EventBusRegistryTest
{

  @Test
  public void testGetDefaultEventBus()
  {
    EventBus eventBus = EventBusRegistry.getEventBus();
    assertSame(eventBus, EventBusRegistry.getEventBus());
  }
  
  @Test
  public void testGetEventBus()
  {
    EventBus eventBus = EventBusRegistry.getEventBus("hansolo");
    assertSame(eventBus, EventBusRegistry.getEventBus("hansolo"));
    assertNotSame(eventBus, EventBusRegistry.getEventBus("hansolo-1"));
  }
  
}
