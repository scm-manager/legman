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



package com.github.legman.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 *
 * @author Sebastian Sdorra
 */
public class GuavaAnnotated
{

  /**
   * Method description
   *
   */
  @Subscribe
  public void methodOne() {}

  /**
   * Method description
   *
   */
  @Subscribe
  @AllowConcurrentEvents
  public void methodTwo() {}
}
