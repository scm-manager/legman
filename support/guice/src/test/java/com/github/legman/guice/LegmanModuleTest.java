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


package com.github.legman.guice;

import com.github.legman.EventBus;
import com.github.legman.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sebastian Sdorra
 */
public class LegmanModuleTest
{

  @Test
  public void testSomeMethod()
  {
    EventBus eventBus = new EventBus();
    Injector injector = Guice.createInjector(
      new LegmanModule(eventBus),
      new AbstractModule(){

      @Override
      protected void configure(){
        bind(SampleService.class).in(Singleton.class);
      }
    });
    
    SampleService service = injector.getInstance(SampleService.class);
    eventBus.post("event");
    assertEquals("event", service.event);
  }
  
  public static class SampleService {
    
    private String event;
    
    @Subscribe(async = false)
    public void handleEvent(String event){
      this.event = event;
    }
    
  }
  
}
