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



package com.github.legman.internal;

//~--- JDK imports ------------------------------------------------------------

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public final class ServiceLocator
{

  private ServiceLocator() {
  }

  public static <T> Iterable<T> locate(Class<T> serviceClass) {
    return ServiceLoader.load(serviceClass, getClassLoader());
  }

  /**
   * Method description
   *
   *
   * @param serviceClass
   * @param provider
   * @param <T>
   *
   * @return
   */
  public static <T> T locateOne(Class<T> serviceClass, ServiceProvider<T> provider)
  {
    T service = locateOne(serviceClass);

    if (service == null)
    {
      service = provider.create();
    }

    return service;
  }

  /**
   * Method description
   *
   *
   * @param serviceClass
   * @param <T>
   *
   * @return
   */
  public static <T> T locateOne(Class<T> serviceClass)
  {
    T service = null;
    Iterator<T> iterator = locate(serviceClass).iterator();

    if (iterator.hasNext())
    {
      service = iterator.next();
    }

    return service;
  }

  /**
   * Method description
   *
   *
   * @param serviceClass
   * @param defaultServiceClass
   * @param <T>
   *
   * @return
   */
  public static <T> T locateOne(Class<T> serviceClass,
                                Class<? extends T> defaultServiceClass)
  {
    T service = locateOne(serviceClass);

    if (service == null)
    {
      try
      {
        service = defaultServiceClass.newInstance();
      }
      catch (Exception ex)
      {
        throw new RuntimeException(
          "could not create default service implementation", ex);
      }
    }

    return service;
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  private static ClassLoader getClassLoader()
  {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    if (classLoader == null)
    {
      classLoader = ServiceLocator.class.getClassLoader();
    }

    return classLoader;
  }

  //~--- inner interfaces -----------------------------------------------------

  /**
   * Interface description
   *
   *
   * @param <T>
   *
   * @version        Enter version here..., 13/07/14
   * @author         Enter your name here...
   */
  public static interface ServiceProvider<T>
  {

    /**
     * Method description
     *
     *
     * @return
     */
    public T create();
  }
}
