package com.github.legman;

import java.lang.reflect.InvocationTargetException;

/**
 * {@link InvocationInterceptor} can be used to do something before or after an event is processed by an subscriber.
 * The interceptor could also be used to prevent an invocation.
 *
 * @since 2.0.0
 */
public interface InvocationInterceptor {

  /**
   * Intercept the invocation of a subscriber. The Interceptor has to call {@link InvocationContext#proceed()} in order
   * to call the next interceptor or to invoke the subscriber.
   *
   * @param context context of the invocation
   * @throws InvocationTargetException is thrown if subscriber could not be invoked
   */
  void invoke(InvocationContext context) throws InvocationTargetException;
}
