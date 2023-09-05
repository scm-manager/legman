package com.github.legman;

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

/**
 * This class is used to guard the executor from being blocked by long-running
 * event handlers. Instead of dispatching more events for such a event handler
 * which might lead to a completely blocked event bus, this class dispatches only
 * one event at a time for a specific event handler (despite if the handler is
 * marked to be concurrent; in this case the events are dispatched as soon as they
 * arrive). Further events are put into a queue, that is taken into account again
 * whenever a process finishes.
 */
class ExecutorSerializer {

  private static final Logger logger = LoggerFactory.getLogger(ExecutorSerializer.class);

  /**
   * The underlying java executor to handle the actual processing.
   */
  private final Executor executor;

  /**
   * Set of handler, that are awaiting execution or currently are executing an event.
   * Further events for handler in this collection are queued in {@link #queuedEvents}.
   */
  private final Set<EventHandler> runningHandlers = new HashSet<>();
  /**
   * Queue of handlers and events that could not have been processed right away, because
   * the handler already is 'busy' with another event.
   */
  private final Queue<EventBus.EventWithHandler> queuedEvents = new LinkedList<>();

  ExecutorSerializer(Executor executor) {
    this.executor = executor;
  }

  /**
   * This takes an event and a handler to dispatch it using the {@link #executor}. If the
   * handler has to be synchronized (aka is marked as non-concurrent, {@link EventHandler#hasToBeSynchronized()}),
   * this is done in the following process, otherwise it is 'put into' the {@link #executor}
   * right away.
   */
  void dispatchAsynchronous(final Object event, final EventHandler wrapper) {
    if (wrapper.hasToBeSynchronized()) {
      executeSynchronized(event, wrapper);
    } else {
      logger.debug("executing handler concurrently: {}", wrapper);
      executor.execute(() -> dispatchDirectly(event, wrapper));
    }
  }

  private void dispatchDirectly(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      logger.error("could not dispatch event: {} to handler {}", event, wrapper, e);
    }
  }

  private synchronized void executeSynchronized(final Object event, final EventHandler wrapper) {
    if (runningHandlers.contains(wrapper)) {
      logger.debug("postponing execution of handler {}; there are already {} other handlers waiting", wrapper, queuedEvents.size());
      queuedEvents.add(new EventBus.EventWithHandler(event, wrapper));
    } else {
      runningHandlers.add(wrapper);
      executor.execute(() -> {
        try {
          dispatchDirectly(event, wrapper);
        } finally {
          releaseRunningHandlerAndTriggerWaitingHandlers(wrapper);
        }
      });
    }
  }

  private synchronized void releaseRunningHandlerAndTriggerWaitingHandlers(EventHandler wrapper) {
    runningHandlers.remove(wrapper);
    logger.debug("checking {} waiting handlers for possible execution", queuedEvents.size());
    for (Iterator<EventBus.EventWithHandler> iterator = queuedEvents.iterator(); iterator.hasNext(); ) {
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

  /**
   * Triggers the shutdown of the {@link #executor} as soon as all events wating inside
   * {@link #queuedEvents} are executed.
   */
  void shutdown() {
    executor.execute(() -> {
      synchronized (this) {
        if (queuedEvents.isEmpty()) {
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
