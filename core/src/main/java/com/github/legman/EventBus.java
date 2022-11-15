/*
 * Copyright (C) 2007 The Guava Authors and SCM-Manager Team
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
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
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
  @SuppressWarnings("UnstableApiUsage")
  private final LoadingCache<Class<?>, Set<Class<?>>> flattenHierarchyCache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(new CacheLoader<Class<?>, Set<Class<?>>>() {
            @SuppressWarnings({"unchecked", "rawtypes"}) // safe cast
            @Override
            public Set<Class<?>> load(@Nonnull Class<?> concreteClass) {
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
  final SetMultimap<Class<?>, EventHandler> handlersByType = HashMultimap.create();

  private final ReadWriteLock handlersByTypeLock = new ReentrantReadWriteLock();

  /**
   * Logger for event dispatch failures.  Named by the fully-qualified name of
   * this class, followed by the identifier provided at construction.
   */
  private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

  /**
   * The {@link AnnotatedHandlerFinder} is suitable for finding subscribers.
   */
  private final AnnotatedHandlerFinder finder;

  /** queues of events for the current thread to dispatch */
  private final ThreadLocal<Queue<EventWithHandler>> eventsToDispatch = ThreadLocal.withInitial(LinkedList::new);

  /** true if the current thread is currently dispatching an event */
  private final ThreadLocal<Boolean> isDispatching = ThreadLocal.withInitial(() -> false);

  /** identifier of the event bus */
  private final String identifier;

  /** executor for handling asynchronous events */
  private final OrderedExecutor executor;

  /** list of invocation interceptors **/
  private final List<InvocationInterceptor> invocationInterceptors;

  /** the queue of asynchronous events is shared across all threads */
  private final ConcurrentLinkedQueue<EventWithHandler> asyncEventsToDispatch =
          new ConcurrentLinkedQueue<>();

  /** name of the default event bus */
  static final String DEFAULT_NAME = "default";

  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  /**
   * Creates a new EventBus named "default".
   */
  public EventBus() {
    this(new Builder());
  }

  /**
   * Creates a new EventBus with the given {@code identifier}.
   *
   * @param identifier  a brief name for this bus, for logging purposes.  Should
   *                    be a valid Java identifier.
   */
  public EventBus(String identifier) {
    this(new Builder().withIdentifier(identifier));
  }

  private EventBus(Builder builder) {
    this.identifier = builder.identifier;
    this.executor = new OrderedExecutor(createExecutor(builder));
    this.invocationInterceptors = Collections.unmodifiableList(builder.invocationInterceptors);
    this.finder = new AnnotatedHandlerFinder();
  }

  private static Executor createExecutor(Builder builder) {
    Executor executor = builder.executor;
    if (executor == null) {
      executor = ExecutorFactory.create(builder.identifier);
    }
    for (ExecutorDecoratorFactory executorDecoratorFactory : builder.executorDecoratorFactories) {
      executor = executorDecoratorFactory.decorate(executor);
    }
    return executor;
  }

  /**
   * Returns the identifier of the EventBus.
   *
   * @return identifier of EventBus.
   */
  String getIdentifier() {
    return identifier;
  }

  /**
   * Registers all handler methods on {@code object} to receive events.
   * Handler methods are selected and classified using {@link AnnotatedHandlerFinder}.
   *
   * @param object  object whose handler methods should be registered.
   */
  public void register(Object object) {
    Multimap<Class<?>, EventHandler> methodsInListener = finder.findAllHandlers(this, object);
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
    if (shutdown.get()) {
      logger.warn("eventbus is already shutdown, we could not process event {}", event);
      return;
    }

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
      logger.debug("found {} expired references, start removing", removable.size());
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
   * {@link #dispatchSynchronousQueuedEvents()} and {@link #dispatchAsynchronousQueuedEvents}.
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
  private void dispatchAsynchronousQueuedEvents() {
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
    if (Boolean.TRUE.equals(isDispatching.get())) {
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
    if (shutdown.get()) {
      logger.warn("eventbus is already shutdown, we could not process event {}", event);
      return;
    }

    if ( wrapper.isAsync() ){
      executor.executeAsync(event, wrapper);
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
  @SuppressWarnings("UnstableApiUsage")
  Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    try {
      return flattenHierarchyCache.getUnchecked(concreteClass);
    } catch (UncheckedExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  /**
   * Stops underlying executor, if it is an instance of {@link ExecutorService}. Have a look at
   * {@link ExecutorService#shutdown()} for more details.
   */
  public void shutdown() {
    shutdown.set(true);
    executor.shutdown();
  }

  /**
   * Returns the list of interceptors.
   * @return list of interceptors
   * @since 2.0.0
   */
  List<InvocationInterceptor> getInvocationInterceptors() {
    return invocationInterceptors;
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

  /**
   * Returns builder for the event bus.
   *
   * @return builder
   * @since 2.0.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for the event bus.
   * @since 2.0.0
   */
  public static class Builder {

    private String identifier = DEFAULT_NAME;
    private Executor executor;
    private final List<ExecutorDecoratorFactory> executorDecoratorFactories = new ArrayList<>();
    private final List<InvocationInterceptor> invocationInterceptors = new ArrayList<>();

    private Builder() {
    }

    /**
     * Sets the identifier for the eventbus.
     *
     * @param identifier identifier
     * @return {@code this}
     */
    public Builder withIdentifier(String identifier) {
      this.identifier = identifier;
      return this;
    }

    /**
     * Sets the {@link Executor} which is used for asynchronous event processing.
     *
     * @param executor executor for asynchronous event processing
     * @return {@code this}
     */
    public Builder withExecutor(Executor executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Decorate the used executor.
     *
     * @param executorDecoratorFactories list of decorator factories
     * @return {@code this}
     */
    public Builder withExecutorDecoratorFactories(ExecutorDecoratorFactory... executorDecoratorFactories) {
      this.executorDecoratorFactories.addAll(Arrays.asList(executorDecoratorFactories));
      return this;
    }

    /**
     * Intercept subscriber invocations.
     *
     * @param invocationInterceptors list of interceptors
     * @return {@code this}
     */
    public Builder withInvocationInterceptors(InvocationInterceptor... invocationInterceptors) {
      this.invocationInterceptors.addAll(Arrays.asList(invocationInterceptors));
      return this;
    }

    /**
     * Apply the list of plugins to eventbus.
     *
     * @param plugins list of plugins
     * @return {@code this}
     */
    public Builder withPlugins(Plugin... plugins) {
      for (Plugin plugin : plugins) {
        plugin.apply(this);
      }
      return this;
    }

    /**
     * Creates the instance of the {@link EventBus}.
     *
     * @return new instance
     */
    public EventBus build() {
      return new EventBus(this);
    }
  }

  private class OrderedExecutor {

    private final Executor delegate;

    private final Set<EventHandler> runningHandlers = new HashSet<>();
    private final Queue<HandlerWithEvent> queuedHandlers = new LinkedList<>();

    private OrderedExecutor(Executor delegate) {
      this.delegate = delegate;
    }

    void executeAsync(final Object event, final EventHandler wrapper) {
      if (wrapper.hasToBeSynchronized()) {
        executeSynchronized(event, wrapper);
      } else {
        logger.warn("executing handler concurrently: {}", wrapper);
        delegate.execute(() -> dispatchSynchronous(event, wrapper));
      }
    }

    private void executeSynchronized(final Object event, final EventHandler wrapper) {
      synchronized (runningHandlers) {
        if (runningHandlers.contains(wrapper)) {
          logger.warn("postponing execution of handler {}; there are already {} other handlers waiting", wrapper, queuedHandlers.size());
          queuedHandlers.add(new HandlerWithEvent(event, wrapper));
        } else {
          runningHandlers.add(wrapper);
          delegate.execute(() -> {
            try {
              dispatchSynchronous(event, wrapper);
            } finally {
              synchronized (runningHandlers) {
                runningHandlers.remove(wrapper);
                triggerWaitingHandlers(wrapper);
              }
            }
          });
        }
      }
    }

    private void triggerWaitingHandlers(EventHandler wrapper) {
      logger.warn("checking {} waiting handlers for possible execution", queuedHandlers.size());
      for (Iterator<HandlerWithEvent> iterator = queuedHandlers.iterator(); iterator.hasNext(); ) {
        HandlerWithEvent queuedHandler = iterator.next();
        if (runningHandlers.contains(queuedHandler.getHandler())) {
          logger.warn("execution of handler still waiting, because other call is still running: {}", wrapper);
        } else {
          logger.warn("executing postponed handler because it is no longer blocked: {}", wrapper);
          iterator.remove();
          executeAsync(queuedHandler.getEvent(), queuedHandler.getHandler());
          break;
        }
      }
    }

    void shutdown() {
      if (delegate instanceof ExecutorService) {
        ((ExecutorService) delegate).shutdown();
      }
    }
  }

  private final class HandlerWithEvent {
    private final Object event;
    private final EventHandler handler;

    public HandlerWithEvent(Object event, EventHandler handler) {
      this.event = event;
      this.handler = handler;
    }

    public Object getEvent() {
      return event;
    }

    public EventHandler getHandler() {
      return handler;
    }
  }
}
