package com.github.legman.micrometer;

import com.github.legman.InvocationContext;
import com.github.legman.InvocationInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.lang.reflect.InvocationTargetException;

public class MicrometerInvocationInterceptor implements InvocationInterceptor {

  private final MeterRegistry registry;

  public MicrometerInvocationInterceptor(MeterRegistry registry) {
    this.registry = registry;
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
            .register(registry);
  }

  private String createTarget(InvocationContext context) {
    return context.getTarget().getClass().getName()
            + "#" + context.getMethod().getName();
  }
}
