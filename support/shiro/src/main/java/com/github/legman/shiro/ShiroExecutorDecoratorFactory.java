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

import com.github.legman.ExecutorDecoratorFactory;
import java.util.concurrent.Executor;
import org.apache.shiro.concurrent.SubjectAwareExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public class ShiroExecutorDecoratorFactory implements ExecutorDecoratorFactory
{
  
  /**
   * the logger for ShiroExecutorDecoratorFactory
   */
  private static final Logger logger = LoggerFactory.getLogger(
    ShiroExecutorDecoratorFactory.class);

  @Override
  public Executor decorate(Executor executor)
  {
    logger.debug(
      "register {} as {} for legman", 
      ShiroExecutorDecoratorFactory.class, 
      ExecutorDecoratorFactory.class
    );
    return new SubjectAwareExecutor(executor);
  }
 
}
