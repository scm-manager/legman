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

package com.github.legman;

import com.github.legman.internal.ServiceLocator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Dispatches events to listeners, and provides ways for listeners to register
 * themselves.
 *
 * <p>The EventBus allows publish-subscribe-style communication between
 * components without requiring the components to explicitly register with one
 * another (and thus be aware of each other).  It is designed exclusively to
 * replace traditional Java in-process event distribution using explicit
 * registration. It is <em>not</em> a general-purpose publish-subscribe system,
 * nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:<ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts
 *     a single argument of the type of event desired;</li>
 * <li>Mark it with a {@link Subscribe} annotation;</li>
 * <li>Pass itself to an EventBus instance's {@link #register(Object)} method.
 *     </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the
 * {@link #post(Object)} method.  The EventBus instance will determine the type
 * of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered
 * to any handler for any type to which the event is <em>assignable.</em>  This
 * includes implemented interfaces, all superclasses, and all interfaces
 * implemented by superclasses.
 *
 * <h2>Handler Methods</h2>
 * Event handler methods must accept only one argument: the event.
 *
 * <p>Handlers should not, in general, throw.  If they do, the EventBus will
 * catch and log the exception.  This is rarely the right solution for error
 * handling and should not be relied upon; it is intended solely to help find
 * problems during development.
 *
 * <p>The EventBus guarantees that it will not call a handler method from
 * multiple threads simultaneously, unless the method explicitly allows it by
 * bearing the {@link Subscribe#allowConcurrentAccess()} attribute.  If this 
 * attribute is not present, handler methods need not worry about being 
 * reentrant, unless also called from outside the EventBus.
 *
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is
 * considered "dead."  To give the system a second chance to handle dead events,
 * they are wrapped in an instance of {@link DeadEvent} and reposted.
 *
 * <p>If a handler for a supertype of all events (such as Object) is registered,
 * no event will ever be considered dead, and no DeadEvents will be generated.
 * Accordingly, while DeadEvent extends {@link Object}, a handler registered to
 * receive any Object will never receive a DeadEvent.
 *
 * <p>This class is safe for concurrent use.
 *
 * @author Cliff Biffle
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public class EventBus {

  /**
   * A thread-safe cache for flattenHierarchy(). The Class class is immutable. This cache is not shared between
   * instances in order to avoid class loader leaks, in environments where classes will be load dynamically.
   */
  private final LoadingCache<Class<?>, Set<Class<?>>> flattenHierarchyCache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(new CacheLoader<Class<?>, Set<Class<?>>>() {
            @SuppressWarnings({"unchecked", "rawtypes"}) // safe cast
            @Override
            public Set<Class<?>> load(Class<?> concreteClass) {
              return (Set) TypeToken.of(concreteClass).getTypes().rawTypes();
            }
          });

  /**
   * All registered event handlers, indexed by event type.
   *
   * <p>This SetMultimap is NOT safe for concurrent use; all access should be
   * made after acquiring a read or write lock via {@link #handlersByTypeLock}.
   */
  @VisibleForTesting
  final SetMultimap<Class<?>, EventHandler> handlersByType =
      HashMultimap.create();
  private final ReadWriteLock handlersByTypeLock = new ReentrantReadWriteLock();

  /**
   * Logger for event dispatch failures.  Named by the fully-qualified name of
   * this class, followed by the identifier provided at construction.
   */
  private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

  /**
   * Strategy for finding handler methods in registered objects. The strategy is
   * loaded with the {@link java.util.ServiceLoader}. The default strategy is the
   * {@link AnnotatedHandlerFinder}.
   */
  private final HandlerFindingStrategy finder =
      ServiceLocator.locate(HandlerFindingStrategy.class, AnnotatedHandlerFinder.class);

  /** queues of events for the current thread to dispatch */
  private final ThreadLocal<Queue<EventWithHandler>> eventsToDispatch =
      new ThreadLocal<Queue<EventWithHandler>>() {
    @Override protected Queue<EventWithHandler> initialValue() {
      return new LinkedList<EventWithHandler>();
    }
  };

  /** true if the current thread is currently dispatching an event */
  private final ThreadLocal<Boolean> isDispatching =
      new ThreadLocal<Boolean>() {
    @Override protected Boolean initialValue() {
      return false;
    }
  };
  
  /** identifier of the event bus */
  private String identifier;

  /** executor for handling asynchronous events */
  private Executor executor;

  /** the queue of asynchronous events is shared across all threads */
  private final ConcurrentLinkedQueue<EventWithHandler> asyncEventsToDispatch =
          new ConcurrentLinkedQueue<EventWithHandler>();

  /** name of the default event bus */
  static final String DEFAULT_NAME = "default";
  
  /**
   * Creates a new EventBus named "default".
   */
  public EventBus() {
    this(DEFAULT_NAME);
  }

  /**
   * Creates a new EventBus with the given {@code identifier}.
   *
   * @param identifier  a brief name for this bus, for logging purposes.  Should
   *                    be a valid Java identifier.
   */
  public EventBus(final String identifier) {
    this.identifier = identifier;

    executor = ServiceLocator.locate( Executor.class, new ServiceLocator.ServiceProvider<Executor>() {
      @Override
      public Executor create() {
        return Executors.newFixedThreadPool(4, 
          new ThreadFactoryBuilder().setNameFormat(
            identifier.concat("-%s")
          ).build()
        );
      }
    });
  }

  /**
   * Returns the identifier of the EventBus.
   * 
   * @return identifier of EventBus.
   */
  String getIdentifier()
  {
    return identifier;
  }

  /**
   * Registers all handler methods on {@code object} to receive events.
   * Handler methods are selected and classified using this EventBus's
   * {@link HandlerFindingStrategy}; the default strategy is the
   * {@link AnnotatedHandlerFinder}.
   *
   * @param object  object whose handler methods should be registered.
   */
  public void register(Object object) {
    Multimap<Class<?>, EventHandler> methodsInListener =
        finder.findAllHandlers(this, object);
    handlersByTypeLock.writeLock().lock();
    try {
      handlersByType.putAll(methodsInListener);
    } finally {
      handlersByTypeLock.writeLock().unlock();
    }
  }

  /**
   * Unregisters all handler methods on a registered {@code object}.
   *
   * @param object  object whose handler methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   */
  public void unregister(Object object) {
    Multimap<Class<?>, EventHandler> methodsInListener = finder.findAllHandlers(this, object);
    for (Entry<Class<?>, Collection<EventHandler>> entry : methodsInListener.asMap().entrySet()) {
      Class<?> eventType = entry.getKey();
      Collection<EventHandler> eventMethodsInListener = entry.getValue();

      handlersByTypeLock.writeLock().lock();
      try {
        Set<EventHandler> currentHandlers = handlersByType.get(eventType);
        if (!currentHandlers.containsAll(eventMethodsInListener)) {
          throw new IllegalArgumentException(
              "missing event handler for an annotated method. Is " + object + " registered?");
        }
        currentHandlers.removeAll(eventMethodsInListener);
      } finally {
        handlersByTypeLock.writeLock().unlock();
      }
    }
  }
  
  /**
   * Remove an registered {@link EventHandler} from the {@link EventBus}.
   * 
   * @param eventHandler event handler which should be removed
   */
  void removeEventHandler(EventHandler eventHandler){
    Entry<Class<?>,EventHandler> entry = null;
    
    for ( Entry<Class<?>,EventHandler> e :  handlersByType.entries() ){
      if ( e.getValue() == eventHandler ){
        entry = e;
        break;
      }
    }
    
    if ( entry != null ){
      handlersByTypeLock.writeLock().lock();
      try {
        if (!handlersByType.remove(entry.getKey(), eventHandler)){
          throw new IllegalArgumentException("event handler could not be removed");
        }
      } finally {
        handlersByTypeLock.writeLock().unlock();
      }
    } else {
      throw new IllegalArgumentException("event handler not found");
    }
  }

  /**
   * Posts an event to all registered handlers.  This method will return
   * successfully after the event has been posted to all handlers, and
   * regardless of any exceptions thrown by handlers.
   *
   * <p>If no handlers have been subscribed for {@code event}'s class, and
   * {@code event} is not already a {@link DeadEvent}, it will be wrapped in a
   * DeadEvent and reposted.
   *
   * @param event  event to post.
   */
  public void post(Object event) {
    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());

    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      handlersByTypeLock.readLock().lock();
      try {
        Set<EventHandler> wrappers = handlersByType.get(eventType);

        if (!wrappers.isEmpty()) {
          dispatched = true;
          for (EventHandler wrapper : wrappers) {
            enqueueEvent(event, wrapper);
          }
        }
      } finally {
        handlersByTypeLock.readLock().unlock();
      }
    }

    if (!dispatched && !(event instanceof DeadEvent)) {
      post(new DeadEvent(this, event));
    }

    dispatchSynchronousQueuedEvents();
    dispatchAsynchronousQueuedEvents();
  }
  
  /**
   * Remove all cleared weak references from eventbus.
   * 
   * @since 1.2.1
   */
  void cleanupWeakReferences()
  {
    logger.trace("start cleanup weak references");
    Set<Entry<Class<?>,EventHandler>> removable = Sets.newHashSet();
    handlersByTypeLock.readLock().lock();
    try {
      for ( Entry<Class<?>, EventHandler> e : handlersByType.entries() )
      {
        if ( e.getValue().getTarget() == null )
        {
          removable.add(e);
        }
      }
    } finally {
      handlersByTypeLock.readLock().unlock();
    }
    if ( ! removable.isEmpty() ){
      logger.debug("found {} expired references, start removing");
      handlersByTypeLock.writeLock().lock();
      try {
        for ( Entry<Class<?>, EventHandler> e : removable )
        {
          handlersByType.remove(e.getKey(), e.getValue());
        }
      } finally {
        handlersByTypeLock.writeLock().unlock();
      }
    } else {
      logger.trace("could not find expired references");
    }
  }

  /**
   * Queue the {@code event} for dispatch during
   * {@link #dispatchSynchronousQueuedEvents()} and {@linl #dispatchAsynchronousQueuedEvents}.
   * Events are queued in-order of occurrence so they can be dispatched in the same order.
   */
  void enqueueEvent(Object event, EventHandler handler) {
    if ( handler.isAsync() ){
      asyncEventsToDispatch.offer(new EventWithHandler(event, handler));
    } else {
      eventsToDispatch.get().offer(new EventWithHandler(event, handler));
    }
  }



  /**
   * Dispatch {@code events} in the order they were posted, regardless of
   * the posting thread.
   */
  @SuppressWarnings("deprecation") // only deprecated for external subclasses
  protected void dispatchAsynchronousQueuedEvents() {
    while (true) {
      EventWithHandler eventWithHandler = asyncEventsToDispatch.poll();
      if (eventWithHandler == null) {
        break;
      }

      dispatch(eventWithHandler.event, eventWithHandler.handler);
    }
  }

  /**
   * Drain the queue of events to be dispatched. As the queue is being drained,
   * new events may be posted to the end of the queue.
   */
  void dispatchSynchronousQueuedEvents() {
    // don't dispatch if we're already dispatching, that would allow reentrancy
    // and out-of-order events. Instead, leave the events to be dispatched
    // after the in-progress dispatch is complete.
    if (isDispatching.get()) {
      return;
    }

    isDispatching.set(true);
    try {
      Queue<EventWithHandler> events = eventsToDispatch.get();
      EventWithHandler eventWithHandler;
      while ((eventWithHandler = events.poll()) != null) {
        dispatch(eventWithHandler.event, eventWithHandler.handler);
      }
    } finally {
      isDispatching.remove();
      eventsToDispatch.remove();
    }
  }

  /**
   * Dispatches {@code event} to the handler in {@code wrapper}.  This method
   * is an appropriate override point for subclasses that wish to make
   * event delivery asynchronous.
   *
   * @param event  event to dispatch.
   * @param wrapper  wrapper that will call the handler.
   */
  void dispatch(final Object event, final EventHandler wrapper) {
    if ( wrapper.isAsync() ){
      executor.execute(new Runnable() {
        @Override
        public void run() {
          dispatchSynchronous(event, wrapper);
        }
      });
    } else {
      dispatchSynchronous(event, wrapper);
    }
  }

  void dispatchSynchronous(Object event, EventHandler wrapper){
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      if ( wrapper.isAsync() ){
        StringBuilder msg = new StringBuilder(identifier);
        msg.append(" - could not dispatch event: ").append(event);
        msg.append(" to handler ").append(wrapper);
        logger.error(msg.toString(), e);
      } else {
        Throwable cause = e.getCause();
        Throwables.propagateIfPossible(cause);
        throw new EventBusException(event, "could not dispatch event", cause);
      }
    }
  }

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set
   * will include all superclasses (transitively), and all interfaces
   * implemented by these superclasses.
   *
   * @param concreteClass  class whose type hierarchy will be retrieved.
   * @return {@code clazz}'s complete type hierarchy, flattened and uniqued.
   */
  @VisibleForTesting
  Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    try {
      return flattenHierarchyCache.getUnchecked(concreteClass);
    } catch (UncheckedExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  /**
   * Stops underlying executor, if it is an instance of {@link ExecutorService}.
   *
   * @see {@link ExecutorService#shutdown()}
   */
  public void shutdown() {
    if (executor instanceof ExecutorService) {
      ((ExecutorService) executor).shutdown();
    }
  }

  /** simple struct representing an event and it's handler */
  static class EventWithHandler {
    final Object event;
    final EventHandler handler;
    public EventWithHandler(Object event, EventHandler handler) {
      this.event = checkNotNull(event);
      this.handler = checkNotNull(handler);
    }
  }
}
