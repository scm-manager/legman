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

import com.github.legman.EventBus;
import com.github.legman.EventBusRegistry;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sebastian Sdorra
 * @since 1.1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventBusRuleTest
{

  private static EventBus eventBus1;
  private static EventBus eventBus2;
  
  @Test
  public void testEventBusRule1()
  {
    eventBus1 = EventBusRegistry.getEventBus();
    assertSame(eventBus1, EventBusRegistry.getEventBus());
  }
  
  @Test
  public void testEventBusRule2()
  {
    eventBus2 = EventBusRegistry.getEventBus();
  }
  
  @Test
  public void testEventBusRule3()
  {
    assertNotSame(eventBus1, eventBus2);
  }
  
  @After
  public void tearDown(){
    
  }
  
  @Rule
  public EventBusRule eventBusRule = new EventBusRule();
  
}
