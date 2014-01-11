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



package sonia.legman.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;

import org.junit.Test;

import org.objectweb.asm.ClassReader;

import sonia.legman.maven.MethodAnnotationClassVisitor.Builder;

import static org.junit.Assert.*;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

/**
 *
 * @author Sebastian Sdorra
 */
public class MethodAnnotationClassVisitorTest
{

  /** Field description */
  private static final String CLASS = GuavaAnnotated.class.getName();

  /** Field description */
  private static final String CLASSFILE =
    "target/test-classes/sonia/legman/maven/GuavaAnnotated.class";

  /** Field description */
  private static final String METHOD_ONE = "methodOne";

  /** Field description */
  private static final String METHOD_TWO = "methodTwo";

  /** Field description */
  private static final String ANNOTATION_TWO =
    AllowConcurrentEvents.class.getName();

  /** Field description */
  private static final String ANNOTATION_ONE = Subscribe.class.getName();

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @throws IOException
   */
  @Test
  public void testMethodAnnotationVisitor() throws IOException
  {
    final List<AnnotatedMethod> list = Lists.newArrayList();
    Builder builder = MethodAnnotationClassVisitor.builder();

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

    try
    {
      stream = new FileInputStream(new File(CLASSFILE));

      ClassReader reader = new ClassReader(stream);

      reader.accept(builder.build(), 0);
    }
    finally
    {
      Closeables.close(stream, true);
    }

    //J-
    AnnotatedMethod[] sorted = Ordering.natural()
      .immutableSortedCopy(list).toArray(new AnnotatedMethod[0]);

    assertArrayEquals(sorted, new AnnotatedMethod[] {
      new AnnotatedMethod(CLASS, METHOD_ONE, ANNOTATION_ONE),
      new AnnotatedMethod(CLASS, METHOD_TWO, ANNOTATION_TWO),
      new AnnotatedMethod(CLASS, METHOD_TWO, ANNOTATION_ONE) 
    });
    //J+
  }

  //~--- inner classes --------------------------------------------------------

  /**
   * Class description
   *
   *
   * @version        Enter version here..., 14/01/11
   * @author         Enter your name here...
   */
  private static class AnnotatedMethod implements Comparable<AnnotatedMethod>
  {

    /**
     * Constructs ...
     *
     *
     * @param className
     * @param methodName
     * @param annotationName
     */
    public AnnotatedMethod(String className, String methodName,
      String annotationName)
    {
      this.className = className;
      this.methodName = methodName;
      this.annotationName = annotationName;
    }

    //~--- methods ------------------------------------------------------------

    /**
     * Method description
     *
     *
     * @param o
     *
     * @return
     */
    @Override
    public int compareTo(AnnotatedMethod o)
    {
      //J-
      return ComparisonChain.start()
        .compare(className,o.className)
        .compare(methodName, o.methodName)
        .compare(annotationName, o.annotationName)
        .result();
      //J+
    }

    /**
     * Method description
     *
     *
     * @param obj
     *
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      final AnnotatedMethod other = (AnnotatedMethod) obj;

      return Objects.equal(className, other.className)
        && Objects.equal(methodName, other.methodName)
        && Objects.equal(annotationName, other.annotationName);
    }

    /**
     * Method description
     *
     *
     * @return
     */
    @Override
    public int hashCode()
    {
      return Objects.hashCode(className, methodName, annotationName);
    }

    /**
     * Method description
     *
     *
     * @return
     */
    @Override
    public String toString()
    {
      //J-
      return Objects.toStringHelper(this)
                    .addValue(className)
                    .addValue(methodName)
                    .addValue(annotationName)
                    .toString();
      //J+
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private final String annotationName;

    /** Field description */
    private final String className;

    /** Field description */
    private final String methodName;
  }
}
