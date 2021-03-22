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


package com.github.legman.guice;

//~--- non-JDK imports --------------------------------------------------------

import com.github.legman.EventBus;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers every bound object to a legman {@link EventBus}.
 *
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public class LegmanModule extends AbstractModule {

  /**
   * the logger for LegmanModule
   */
  private static final Logger LOG = LoggerFactory.getLogger(LegmanModule.class);

  public LegmanModule(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  protected void configure() {
    bindListener(Matchers.any(), new TypeListener() {

      @Override
      public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(new InjectionListener<I>() {

          @Override
          public void afterInjection(I injectee) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("register subscriber {}", injectee.getClass());
            }
            eventBus.register(injectee);
          }
        });
      }

    });
  }

  private final EventBus eventBus;
}
