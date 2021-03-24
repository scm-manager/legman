/*
 * Copyright (C) 2013 SCM-Manager Team
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
