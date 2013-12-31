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


package com.github.legman.shiro;

import com.github.legman.EventBus;
import com.github.legman.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import sonia.junit.shiro.ShiroRule;
import sonia.junit.shiro.SubjectAware;

/**
 *
 * @author Sebastian Sdorra
 */
@SubjectAware(configuration = "classpath:com/github/legman/shiro/001.ini")
public class ShiroExecutorDecoratorFactoryTest
{

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void testSubjectAware() throws InterruptedException
  {
    EventBus bus = new EventBus();
    Listener listener = new Listener();
    bus.register(listener);
    bus.post("event");
    
    // wait until async event is dispatched
    Thread.sleep(500l);
    assertEquals("trillian", listener.principal);
  }
  
  private static class Listener {
    
    private Object principal;
    
    @Subscribe
    public void handleEvent(String event){
      principal = SecurityUtils.getSubject().getPrincipal();
    }
    
  }
  
  @Rule
  public ShiroRule rule = new ShiroRule();
}
