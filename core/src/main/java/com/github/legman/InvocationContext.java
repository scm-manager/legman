package com.github.legman;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Context object for {@link InvocationInterceptor}.
 *
 * @since 2.0.0
 */
public final class InvocationContext {

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

  /**
   * Returns the identifier of the event bus.
   *
   * @return eventbus identifier
   */
  public String getEventBusIdentifier() {
    return eventBusIdentifier;
  }

  /**
   * Returns the subscriber method which will be invoked.
   *
   * @return subscriber method
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Returns the subscriber object on which the method will be invoked.
   *
   * @return subscriber object
   */
  public Object getTarget() {
    return target;
  }

  /**
   * Returns the event object which will be passed to the subscriber method.
   *
   * @return event object
   */
  public Object getEvent() {
    return event;
  }

  /**
   * Returns {@code true} if the event is processed asynchronous.
   *
   * @return {@code true} if the event is processed asynchronous
   */
  public boolean isAsynchronous() {
    return asynchronous;
  }

  /**
   * Calls the next interceptor in the chain or invoke the subscriber method.
   *
   * @throws InvocationTargetException is thrown if subscriber could not be invoked
   */
  public void proceed() throws InvocationTargetException {
    if (interceptors.hasNext()) {
      InvocationInterceptor interceptor = interceptors.next();
      interceptor.invoke(this);
    } else {
      invoke();
    }
  }

  @SuppressWarnings("java:S112")
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
