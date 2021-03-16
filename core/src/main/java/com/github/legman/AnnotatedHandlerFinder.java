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

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import javax.annotation.Nullable;

/**
 * Collecting all event handler methods that are marked with the {@link Subscribe} annotation.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 * @author Sebastian Sdorra
 * @since 1.0.0
 */
public class AnnotatedHandlerFinder {
  /**
   * A thread-safe cache that contains the mapping from each class to all methods in that class and
   * all super-classes, that are annotated with {@code @Subscribe}. This cache is not shared between instances in order
   * to avoid class loader leaks, in environments where classes will be load dynamically.
   */
  private final LoadingCache<Class<?>, ImmutableList<EventMetadata>> handlerMethodsCache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(new CacheLoader<Class<?>, ImmutableList<EventMetadata>>() {
            @Override
            public ImmutableList<EventMetadata> load(Class<?> concreteClass) throws Exception {
              return getMetadataInternal(concreteClass);
            }
          });

  /**
   * Finds all suitable event handler methods in {@code source}, organizes them
   * by the type of event they handle, and wraps them in {@link EventHandler} instances.
   *
   * @param eventBus event bus
   * @param listener  object whose handlers are desired.
   * @return EventHandler objects for each handler method, organized by event
   *         type.
   *
   * @throws IllegalArgumentException if {@code source} is not appropriate for
   *         this strategy (in ways that this interface does not define).
   */
  public Multimap<Class<?>, EventHandler> findAllHandlers(EventBus eventBus, Object listener) {
    Multimap<Class<?>, EventHandler> methodsInListener = HashMultimap.create();
    Class<?> clazz = listener.getClass();
    for (EventMetadata metadata : getEventMetadata(clazz)) {
      Class<?>[] parameterTypes = metadata.method.getParameterTypes();
      Class<?> eventType = parameterTypes[0];
      EventHandler handler = makeHandler(eventBus, listener, metadata);
      methodsInListener.put(eventType, handler);
    }
    return methodsInListener;
  }

  private ImmutableList<EventMetadata> getEventMetadata(Class<?> clazz) {
    try {
      return handlerMethodsCache.getUnchecked(clazz);
    } catch (UncheckedExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  private static final class MethodIdentifier {
    private final String name;
    private final List<Class<?>> parameterTypes;

    MethodIdentifier(Method method) {
      this.name = method.getName();
      this.parameterTypes = Arrays.asList(method.getParameterTypes());
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameterTypes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof MethodIdentifier) {
        MethodIdentifier ident = (MethodIdentifier) o;
        return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
      }
      return false;
    }
  }

  private static ImmutableList<EventMetadata> getMetadataInternal(Class<?> clazz) {
    Set<? extends Class<?>> supers = TypeToken.of(clazz).getTypes().rawTypes();
    Map<MethodIdentifier, EventMetadata> identifiers = Maps.newHashMap();
    for (Class<?> superClazz : supers) {
      for (Method superClazzMethod : superClazz.getMethods()) {
        Subscribe subscribe = getSubscribeAnnotation(superClazzMethod);
        if (subscribe != null) {
          Class<?>[] parameterTypes = superClazzMethod.getParameterTypes();
          if (parameterTypes.length != 1) {
            throw new IllegalArgumentException("Method " + superClazzMethod
                + " has @Subscribe annotation, but requires " + parameterTypes.length
                + " arguments.  Event handler methods must require a single argument.");
          }

          MethodIdentifier ident = new MethodIdentifier(superClazzMethod);
          if (!identifiers.containsKey(ident)) {
            identifiers.put(ident,
              new EventMetadata(
                superClazzMethod,
                subscribe.referenceType(),
                subscribe.allowConcurrentAccess(),
                subscribe.async()
              )
            );
          }
        }
      }
    }
    return ImmutableList.copyOf(identifiers.values());
  }

  private static Subscribe getSubscribeAnnotation(Method method)
  {
    Subscribe subscribe = null;
    for (Annotation annotation : method.getDeclaredAnnotations()){
      if (annotation instanceof Subscribe){
        subscribe = (Subscribe) annotation;
      }
    }
    return subscribe;
  }

  /**
   * Creates an {@code EventHandler} for subsequently calling {@code method} on
   * {@code listener}.
   * Selects an EventHandler implementation based on the annotations on
   * {@code method}.
   *
   * @param listener  object bearing the event handler method.
   * @param metadata  the event metadata to wrap in an EventHandler.
   * @return an EventHandler that will call {@code method} on {@code listener}
   *         when invoked.
   */
  private static EventHandler makeHandler(EventBus eventBus, Object listener, EventMetadata metadata) {
    EventHandler wrapper;
    if (metadata.allowConcurrentAccess) {
      wrapper = new EventHandler(
        eventBus,
        listener,
        metadata.method,
        metadata.referenceType,
        metadata.async
      );
    } else {
      wrapper = new SynchronizedEventHandler(
        eventBus,
        listener,
        metadata.method,
        metadata.referenceType,
        metadata.async
      );
    }
    return wrapper;
  }

  private static class EventMetadata {

    private Method method;
    private ReferenceType referenceType;
    private boolean allowConcurrentAccess;
    private boolean async;

    private EventMetadata(Method method, ReferenceType referenceType, boolean allowConcurrentAccess,  boolean async) {
      this.method = method;
      this.referenceType = referenceType;
      this.allowConcurrentAccess = allowConcurrentAccess;
      this.async = async;
    }
  }
}
