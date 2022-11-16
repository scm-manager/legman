package com.github.legman;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

class OrderedExecutor {

  private static final Logger logger = LoggerFactory.getLogger(OrderedExecutor.class);

  private final Executor executor;

  private final Set<EventHandler> runningHandlers = new HashSet<>();
  private final Queue<EventBus.EventWithHandler> queuedHandlers = new LinkedList<>();

  OrderedExecutor(Executor executor) {
    this.executor = executor;
  }

  void dispatchAsynchronous(final Object event, final EventHandler wrapper) {
    if (wrapper.hasToBeSynchronized()) {
      executeSynchronized(event, wrapper);
    } else {
      logger.debug("executing handler concurrently: {}", wrapper);
      executor.execute(() -> dispatchDirectly(event, wrapper));
    }
  }

  void dispatchDirectly(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      if (wrapper.isAsync()) {
        logger.error("could not dispatch event: {} to handler {}", event, wrapper, e);
      } else {
        Throwable cause = e.getCause();
        Throwables.propagateIfPossible(cause);
        throw new EventBusException(event, "could not dispatch event", cause);
      }
    }
  }

  private void executeSynchronized(final Object event, final EventHandler wrapper) {
    synchronized (this) {
      if (runningHandlers.contains(wrapper)) {
        logger.debug("postponing execution of handler {}; there are already {} other handlers waiting", wrapper, queuedHandlers.size());
        queuedHandlers.add(new EventBus.EventWithHandler(event, wrapper));
      } else {
        runningHandlers.add(wrapper);
        executor.execute(() -> {
          try {
            dispatchDirectly(event, wrapper);
          } finally {
            synchronized (this) {
              runningHandlers.remove(wrapper);
              triggerWaitingHandlers(wrapper);
            }
          }
        });
      }
    }
  }

  private void triggerWaitingHandlers(EventHandler wrapper) {
    logger.debug("checking {} waiting handlers for possible execution", queuedHandlers.size());
    for (Iterator<EventBus.EventWithHandler> iterator = queuedHandlers.iterator(); iterator.hasNext(); ) {
      EventBus.EventWithHandler queuedHandler = iterator.next();
      if (runningHandlers.contains(queuedHandler.handler)) {
        logger.debug("execution of handler still waiting, because other call is still running: {}", wrapper);
      } else {
        logger.debug("executing postponed handler because it is no longer blocked: {}", wrapper);
        iterator.remove();
        executeSynchronized(queuedHandler.event, queuedHandler.handler);
        break;
      }
    }
  }

  void shutdown() {
    executor.execute(() -> {
      synchronized (this) {
        if (queuedHandlers.isEmpty()) {
          logger.debug("no more handlers queued; shutting down executors");
          if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
          }
        } else {
          logger.debug("queued handlers found; postponing shutdown");
          shutdown();
        }
      }
    });
  }
}
