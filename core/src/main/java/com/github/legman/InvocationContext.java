package com.github.legman;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class InvocationContext {

  private final Iterator<InvocationInterceptor> interceptors;
  private final String eventBusIdentifier;
  private final Method method;
  private final Object target;
  private final Object event;
  private final boolean asynchronous;

  InvocationContext(EventBus eventBus, Method method, Object target, Object event, boolean asynchronous) {
    this.interceptors = eventBus.getInvocationInterceptors().iterator();
    this.eventBusIdentifier = eventBus.getIdentifier();
    this.method = method;
    this.target = target;
    this.event = event;
    this.asynchronous = asynchronous;
  }

  public String getEventBusIdentifier() {
    return eventBusIdentifier;
  }

  public Method getMethod() {
    return method;
  }

  public Object getTarget() {
    return target;
  }

  public Object getEvent() {
    return event;
  }

  public boolean isAsynchronous() {
    return asynchronous;
  }

  public void proceed() throws InvocationTargetException {
    if (interceptors.hasNext()) {
      InvocationInterceptor interceptor = interceptors.next();
      interceptor.invoke(this);
    } else {
      invoke();
    }
  }

  private void invoke() throws InvocationTargetException {
    try {
      method.invoke(target, event);
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
  }
}
