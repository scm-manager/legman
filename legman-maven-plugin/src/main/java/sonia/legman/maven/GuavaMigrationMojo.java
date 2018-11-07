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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import sonia.legman.maven.MethodAnnotationClassVisitor.Builder;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Sebastian Sdorra
 * @goal guava-migration-check
 * @phase process-classes
 * @requiresDependencyResolution runtime
 */
public class GuavaMigrationMojo extends AbstractMojo
{

  /**
   * Method description
   *
   *
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    try
    {
      if (classesDirectory.exists()) {
        process(classesDirectory);
      } else {
        getLog().info("skip guava migration check, because the classes directory could not be found");
      }
    }
    catch (IOException e)
    {
      throw new MojoExecutionException("Failed to process guava annotations",
        e);
    }
    catch (MigrationViolationException ex) {
      throw new MojoFailureException(ex.getMessage());
    }
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param classesDirectory
   */
  public void setClassesDirectory(File classesDirectory)
  {
    this.classesDirectory = classesDirectory;
  }

  /**
   * Method description
   *
   *
   * @param template
   */
  public void setTemplate(String template)
  {
    this.template = template;
  }

  public void setFail(boolean fail) {
    this.fail = fail;
  }


  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param file
   *
   * @throws IOException
   */
  void processClassFile(File file) throws IOException
  {
    InputStream stream = null;

    try
    {
      stream = new FileInputStream(file);

      ClassReader reader = new ClassReader(stream);

      Builder builder = MethodAnnotationClassVisitor.builder();

      builder.api(Opcodes.ASM5);
      builder.annotateClasses(Subscribe.class, AllowConcurrentEvents.class);
      builder.methodAnnotationHandler(new MethodAnnotationHandler()
      {

        @Override
        public void handleMethodAnnotation(String className, String methodName,
          String annotationName)
        {
          //J-
          String message = template
            .replace("{class}", className)
            .replace("{method}", methodName)
            .replace("{annotation}", annotationName)
            .replace("{subscribe}", com.github.legman.Subscribe.class.getName());
          //J+ 

          handleViolation(message);
        }
      });

      reader.accept(builder.build(), 0);
    }
    finally
    {
      Closeables.close(stream, true);
    }
  }

  private void handleViolation(String message) {
    if (fail) {
      throw new MigrationViolationException(message);
    } else {
      getLog().warn(message);
    }
  }

  /**
   * Method description
   *
   *
   * @param dir
   *
   * @throws IOException
   */
  private void process(File dir) throws IOException
  {
    for (File f : dir.listFiles())
    {
      if (f.isDirectory())
      {
        process(f);
      }
      else if (f.getName().endsWith(".class"))
      {
        try
        {
          processClassFile(f);
        }
        catch (IOException ex)
        {
          throw new IOException("Failed to process " + f, ex);
        }
      }
    }
  }

  //~--- fields ---------------------------------------------------------------

  /**
   * The directory containing generated classes.
   *
   * @parameter expression="${project.build.directory}/classes"
   * @required
   */
  private File classesDirectory;

  /**
   * @parameter
   */
  private String template =
    "method {method} of class {class} uses {annotation}, please use {subscribe} annotation";

  /**
   * Fail if an guava eventbus annotation is found.
   *
   * @parameter
   */
  private boolean fail = false;
}
