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



package com.github.legman.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//~--- JDK imports ------------------------------------------------------------

import java.lang.annotation.Annotation;

import java.util.Set;

/**
 *
 * @author Sebastian Sdorra
 */
public class MethodAnnotationClassVisitor extends ClassVisitor
{

  /**
   * Constructs ...
   *
   *
   * @param api
   * @param methodAnnotationHandler
   * @param annotationClasses
   */
  private MethodAnnotationClassVisitor(int api,
    MethodAnnotationHandler methodAnnotationHandler,
    Iterable<Class<? extends Annotation>> annotationClasses)
  {
    super(api);
    this.methodAnnotationHandler = methodAnnotationHandler;

    ImmutableSet.Builder<String> builder = ImmutableSet.builder();

    for (Class<? extends Annotation> annotationClass : annotationClasses)
    {
      builder.add(annotationClass.getName());
    }

    annotations = builder.build();
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  public static Builder builder()
  {
    return new Builder();
  }

  /**
   * Method description
   *
   *
   * @param version
   * @param access
   * @param name
   * @param signature
   * @param superName
   * @param interfaces
   */
  @Override
  public void visit(int version, int access, String name, String signature,
    String superName, String[] interfaces)
  {
    this.className = transformDescToClassName(name);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Method description
   *
   *
   * @param access
   * @param methodName
   * @param desc
   * @param signature
   * @param exceptions
   *
   * @return
   */
  @Override
  public MethodVisitor visitMethod(int access, final String methodName,
    String desc, String signature, String[] exceptions)
  {
    return new MethodVisitor(api)
    {

      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible)
      {
        String annotation = transformDescToClassName(desc);

        if (annotations.contains(annotation))
        {
          methodAnnotationHandler.handleMethodAnnotation(className, methodName,
            annotation);
        }

        return super.visitAnnotation(desc, visible);
      }

    };
  }

  /**
   * Method description
   *
   *
   * @param desc
   *
   * @return
   */
  private String transformDescToClassName(String desc)
  {
    if (desc.startsWith("L") && desc.endsWith(";"))
    {
      desc = desc.substring(1).substring(0, desc.length() - 2);
    }

    return desc.replace('/', '.');
  }

  //~--- inner classes --------------------------------------------------------

  /**
   * Class description
   *
   *
   * @version        Enter version here..., 14/01/11
   * @author         Enter your name here...
   */
  public static class Builder
  {

    /**
     * Method description
     *
     *
     * @param annotationClass
     * @param annotationClasses
     *
     * @return
     */
    public Builder annotateClasses(Class<? extends Annotation> annotationClass,
      Class<? extends Annotation>... annotationClasses)
    {
      this.annotationClasses.addAll(Lists.asList(annotationClass,
        annotationClasses));

      return this;
    }

    /**
     * Method description
     *
     *
     * @param api
     *
     * @return
     */
    public Builder api(int api)
    {
      this.api = api;

      return this;
    }

    /**
     * Method description
     *
     *
     * @return
     */
    public MethodAnnotationClassVisitor build()
    {
      return new MethodAnnotationClassVisitor(api, methodAnnotationHandler,
        annotationClasses);
    }

    /**
     * Method description
     *
     *
     * @param methodAnnotationHandler
     *
     * @return
     */
    public Builder methodAnnotationHandler(
      MethodAnnotationHandler methodAnnotationHandler)
    {
      this.methodAnnotationHandler = methodAnnotationHandler;

      return this;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private int api = Opcodes.ASM5;

    /** Field description */
    private final Set<Class<? extends Annotation>> annotationClasses =
      Sets.newHashSet();

    /** Field description */
    private MethodAnnotationHandler methodAnnotationHandler;
  }


  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private final Set<String> annotations;

  /** Field description */
  private final MethodAnnotationHandler methodAnnotationHandler;

  /** Field description */
  private String className;
}
