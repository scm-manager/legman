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

//~--- non-JDK imports --------------------------------------------------------

import com.github.legman.EventBusRegistryProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Binds a new {@link EventBusRegistryProvider} to the current thread before 
 * each unit test.
 *
 * @author Sebastian Sdorra
 * @since 1.1.0
 */
public class EventBusRule implements MethodRule
{

  /**
   * Initializes the {@link ThreadLocalEventBusRegistryProvider} before the 
   * method is executed and releases the 
   * {@link ThreadLocalEventBusRegistryProvider} after the method is finished.
   *
   *
   * @param base base statement
   * @param method framework method
   * @param target target object
   *
   * @return wrapped statement
   */
  @Override
  public Statement apply(final Statement base, FrameworkMethod method,
    Object target)
  {
    return new Statement()
    {

      @Override
      public void evaluate() throws Throwable
      {
        ThreadLocalEventBusRegistryProvider.initialize();

        try
        {
          base.evaluate();
        }
        finally
        {
          ThreadLocalEventBusRegistryProvider.release();
        }
      }
    };
  }
}
