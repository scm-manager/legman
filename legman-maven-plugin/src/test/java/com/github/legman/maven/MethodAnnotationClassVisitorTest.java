/*
 * Copyright (C) 2013 SCM-Manager Team
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

//~--- JDK imports ------------------------------------------------------------

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Sebastian Sdorra
 */
class MethodAnnotationClassVisitorTest {

  private static final String CLASS = GuavaAnnotated.class.getName();

  private static final String CLASSFILE =
    "target/test-classes/com/github/legman/maven/GuavaAnnotated.class";

  private static final String METHOD_ONE = "methodOne";
  private static final String METHOD_TWO = "methodTwo";
  private static final String ANNOTATION_TWO = AllowConcurrentEvents.class.getName();
  private static final String ANNOTATION_ONE = Subscribe.class.getName();

  @Test
  void testMethodAnnotationVisitor() throws IOException {
    final List<AnnotatedMethod> list = Lists.newArrayList();
    MethodAnnotationClassVisitor.Builder builder = MethodAnnotationClassVisitor.builder();

    builder.annotateClasses(Subscribe.class, AllowConcurrentEvents.class);
    builder.methodAnnotationHandler(new MethodAnnotationHandler()
    {

      @Override
      public void handleMethodAnnotation(String className, String methodName,
        String annotationName)
      {
        list.add(new AnnotatedMethod(className, methodName, annotationName));
      }
    });

    InputStream stream = null;

    try {
      stream = new FileInputStream(CLASSFILE);

      ClassReader reader = new ClassReader(stream);

      reader.accept(builder.build(), 0);
    } finally {
      Closeables.close(stream, true);
    }

    AnnotatedMethod[] sorted = Ordering.natural()
            .immutableSortedCopy(list)
            .toArray(new AnnotatedMethod[0]);

    assertThat(sorted).containsExactly(
      new AnnotatedMethod(CLASS, METHOD_ONE, ANNOTATION_ONE),
      new AnnotatedMethod(CLASS, METHOD_TWO, ANNOTATION_TWO),
      new AnnotatedMethod(CLASS, METHOD_TWO, ANNOTATION_ONE)
    );
  }

  private static class AnnotatedMethod implements Comparable<AnnotatedMethod> {

    private final String className;
    private final String methodName;
    private final String annotationName;

    public AnnotatedMethod(String className, String methodName, String annotationName) {
      this.className = className;
      this.methodName = methodName;
      this.annotationName = annotationName;
    }

    @Override
    public int compareTo(AnnotatedMethod o) {
      return ComparisonChain.start()
        .compare(className,o.className)
        .compare(methodName, o.methodName)
        .compare(annotationName, o.annotationName)
        .result();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      final AnnotatedMethod other = (AnnotatedMethod) obj;

      return Objects.equals(className, other.className)
        && Objects.equals(methodName, other.methodName)
        && Objects.equals(annotationName, other.annotationName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(className, methodName, annotationName);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                    .addValue(className)
                    .addValue(methodName)
                    .addValue(annotationName)
                    .toString();
    }

  }
}
