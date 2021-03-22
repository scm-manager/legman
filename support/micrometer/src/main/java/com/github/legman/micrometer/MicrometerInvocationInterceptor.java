/*
 * Copyright (C) 2007 The Guava Authors and Sebastian Sdorra
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

package com.github.legman.micrometer;

import com.github.legman.InvocationContext;
import com.github.legman.InvocationInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.lang.reflect.InvocationTargetException;

public class MicrometerInvocationInterceptor implements InvocationInterceptor {

  private final MeterRegistry registry;
  private final Iterable<Tag> tags;

  MicrometerInvocationInterceptor(MeterRegistry registry, Iterable<Tag> tags) {
    this.registry = registry;
    this.tags = tags;
  }

  @Override
  public void invoke(InvocationContext context) throws InvocationTargetException {
    Timer.Sample timer = Timer.start(registry);
    String exception = "none";
    try {
      context.proceed();
    } catch (Exception ex){
      exception = ex.getClass().getName();
      throw ex;
    } finally {
      timer.stop(timer(context, exception));
    }
  }

  private Timer timer(InvocationContext context, String exception) {
    return Timer.builder("legman.invocation")
            .description("Subscriber invocation timer")
            .tag("bus", context.getEventBusIdentifier())
            .tag("event", context.getEvent().getClass().getName())
            .tag("target", createTarget(context))
            .tag("exception", exception)
            .tags(tags)
            .register(registry);
  }

  private String createTarget(InvocationContext context) {
    return context.getTarget().getClass().getName()
            + "#" + context.getMethod().getName();
  }
}
