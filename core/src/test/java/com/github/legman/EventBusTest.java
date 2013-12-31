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


public class EventBusTest
{
  
  private String weakReferenceTest;
  
  private String strongReferenceTest;
  
  @Test
  public void testWeakReference()
  {
    EventBus bus = new EventBus();
    bus.register(new WeakListener());
    System.gc();
    bus.post("event");
    assertNull(weakReferenceTest);
  }
  
  @Test
  public void testStrongReference()
  {
    EventBus bus = new EventBus();
    bus.register(new StrongListener());
    System.gc();
    bus.post("event");
    assertEquals("event", strongReferenceTest);
  }
  
  @Test
  public void testSyncListener()
  {
    EventBus bus = new EventBus();
    SyncListener listener = new SyncListener();
    bus.register(listener);
    bus.post("event");
    assertEquals("event", listener.event);
  }
  
  @Test
  public void testAsyncListener() throws InterruptedException
  {
    EventBus bus = new EventBus();
    AsyncListener listener = new AsyncListener();
    bus.register(listener);
    bus.post("event");
    assertNull(listener.event);
    Thread.sleep(500l);
    assertEquals("event", listener.event);
  }
  
  @Test
  public void testDeadEvent(){
    EventBus bus = new EventBus();
    SyncListener listener = new SyncListener();
    bus.register(listener);
    DeadEventListener deadEventListener = new DeadEventListener();
    bus.register(deadEventListener);
    bus.post(new Integer(12));
    assertNotNull(deadEventListener.event);
  }
  
  private class DeadEventListener {
    
    private DeadEvent event;
    
    @Subscribe(async = false)
    public void handleEvent(DeadEvent event){
      this.event = event;
    }
    
  }

  private class AsyncListener {
    
    private String event;
    
    @Subscribe
    public void handleEvent(String event){
      this.event = event;
    }
  }
  
  private class SyncListener {
    
    private String event;
    
    @Subscribe(async = false)
    public void handleEvent(String event){
      this.event = event;
    }
  }
  
  private class StrongListener {
    
    @Subscribe(async = false, referenceType = ReferenceType.STRONG)
    public void handleEvent(String event){
      strongReferenceTest = event;
    } 
  }
  
  private class WeakListener {
    
    @Subscribe(async = false)
    public void handleEvent(String event){
      weakReferenceTest = event;
    }
  }
  
}
