/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import  java.io.File;
import  java.io.StringWriter;
import  java.util.LinkedList;
import  java.util.Map;

import  net.sourceforge.cruisecontrol.Builder;
import  net.sourceforge.cruisecontrol.CruiseControlException;
import  net.sourceforge.cruisecontrol.Progress;
import  net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import  net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import  net.sourceforge.cruisecontrol.util.DateUtil;
import  net.sourceforge.cruisecontrol.util.IO;
import  net.sourceforge.cruisecontrol.util.OSEnvironment;
import  net.sourceforge.cruisecontrol.util.ValidationHelper;

import  org.apache.log4j.Logger;
import  org.jdom.Attribute;
import  org.jdom.Element;


/**
 * Builder class for CMake C/C++ build system.
 *
 * Executes <tt>cmake</tt> command with a given parameters, followed by the user-defined sequence
 * of commands. Builder fails if any of the commands fail.
 *
 * Although there is possibility to use {@link CompositeBuilder}, this class has the
 * following advantages:
 * <ul>
 * <li> more user-friendly format than when using raw {@link ExecBuilder}:
 *      <pre>
 *        <exec command="rm" args="-f /path/to/build/dir"/>
 *        <exec command="mkdir" args="/path/to/build/dir"/>
 *        <exec command="cmake"
 *              args="-D CMAKE_VERBOSE_MAKEFILE:BOOL=ON -D.... -D INSTALL_PREFIX=/usr/local/bin
 *                    -U USER_UNDEF, -U ... -G 'Unix Makefiles' /path/to/source/root"
 *              workingdir="/path/to/build/dir"/>
 *        <exec command="make"
 *              workingdir="/path/to/build/dir"/>
 *        <exec command="make" args="install"
 *              workingdir="/path/to/build/dir"/>
 *      </pre>
 *      will look with CMake builder as follows:
 *      <pre>
 *        <cmake srcroot="/path/to/source/root"
 *               builddir="/path/to/build/dir"  cleanbuild="true">
 *
 *              <option value="-D CMAKE_VERBOSE_MAKEFILE:BOOL=ON"/>
 *              ...
 *              <option value="-D INSTALL_PREFIX=/usr/local/bin"/>
 *              <option value="-U USER_UNDEF"/>
 *              ...
 *              <option value="-G 'Unix Makefiles'"/>
 *
 *              <build  exec="make" />
 *              <build  exec="make" args="install"/>
 *        </cmake>
 *      </pre>
 * <li> CMake can be pre-configured using <tt><plugin /></tt> framework
 * <li> the build directory is automatically created if not existing.
 * <li> each build can start in empty build directory, see {@link #setCleanBuild(boolean)}.
 * <li> the output of <tt>make test</tt> can be integrated to the output of CC as it is in case of
 *      ANT builder (<b>not finished yet!</b>)
 * </ul>
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
public class CMakeBuilder extends Builder {

  /**
   * Validate the attributes for the plugin.
   */
  @Override
  public void validate() throws CruiseControlException {
    super.validate();

    /* A top-level CMake file and build directory arguments are required to be defined */
    ValidationHelper.assertIsSet(buildDir, "builddir", this.getClass());
    ValidationHelper.assertIsSet(srcRoot, "srcroot", this.getClass());

    /* Check the existence of source directory and CMakeLists.txt file in it. */
    ValidationHelper.assertExists(srcRoot, "srcroot", this.getClass());
    ValidationHelper.assertExists(new File(srcRoot, "CMakeLists.txt"), "srcroot", this.getClass());
    /* There must not be a file with the name set in buildDir */
    ValidationHelper.assertFalse(buildDir.isFile(), "There is file '" + buildDir
            + "existing, but it is required to be build directory");

    /* Validate all the cmake defines */
    for (Option option : options) {
         ValidationHelper.assertNotEmpty(option.toString(), "option", option.getClass());
    }

    /* Build the commands to execute. The first is "raw" cmake */
    final ExecBuilder builder = new ExecBuilderCMake();
    final StringBuilder args = new StringBuilder();

    /* Options for CMake */
    for (Option option : options) {
         args.append(option.toString() + " ");
    }
    /* srcRoot - path to CMakeLists.txt */
    args.append(srcRoot.getAbsolutePath());

    builder.setCommand("cmake");
    builder.setArgs(args.toString());

    /* CMake is the very first */
    commands.addFirst(builder);

    /* Set inherited properties and validate individual commands */
    for (ExecBuilder c : commands) {
         if (c.getWorkingDir() == null) {
             c.setWorkingDir(buildDir.getAbsolutePath());
         }
         c.setLiveOutput(isLiveOutput());
         c.setMultiple(getMultiple());
         c.setShowProgress(getShowProgress());
         // TODO setDate(), setTime()??

         /* Validate */
         c.validate();
    }
  }

  /**
   * Executes the commands and return the results as XML
   */
  @Override
  public Element build(final Map<String, String> buildProperties, final Progress progressIn)
          throws CruiseControlException {
    long startTime = System.currentTimeMillis();
    Element buildLogElement = new Element("build");
    Element cmmndLogElement;
    Attribute cmndErrAttrib;

    /* If clean directory is required, clean it */
    if (cleanBuild) {
        IO.delete(buildDir);
    }
    /* Creates the build directory, if it does not exist. */
    if (!buildDir.exists()) {
        buildDir.mkdirs();
    }

    /* Call all the commands one after another */
    for (ExecBuilder c : commands) {
        long remainTime = timeout != ScriptRunner.NO_TIMEOUT
                                  ?  timeout - (System.currentTimeMillis() - startTime) / 1000
                                  :  Long.MAX_VALUE;

        /* timeout - it must be set dynamically ...*/
        if (c.getTimeout() == ScriptRunner.NO_TIMEOUT || c.getTimeout() > remainTime) {
            c.setTimeout(remainTime);
        }

        /* start the command and store its output into the overall logElement */
        cmmndLogElement = c.build(buildProperties, progressIn);
        buildLogElement.addContent(cmmndLogElement);
        /* if the result contains "error" attribute, I suppose it failed. Copy the error to the top-level
           to signalize CC that something went wrong, and the build failed */
        cmndErrAttrib = cmmndLogElement.getAttribute("error");
        if (cmndErrAttrib != null) {
             buildLogElement.setAttribute(cmndErrAttrib.detach());
             break;
        }
    }

    /* Set the time it took to exec the whole command. Taken from ExecBuilder#build() */
    buildLogElement.setAttribute("time", DateUtil.getDurationAsString((System.currentTimeMillis() - startTime)));
    /* Return the whole build log */
    return buildLogElement;
  }


  /**
   * ????
   */
  @Override
  public Element buildWithTarget(final Map<String, String> properties, final String target,
          final Progress progress) throws CruiseControlException {
    // TODO: finish it
    throw new CruiseControlException("Method not implemented! I do not understand its difference from #build()");
  }

  /**
   * Sets the directory where the top-level <tt>CMakeLists.txt</tt> is located. The attribute
   * is required.
   *
   * @param path the path to the top-level <tt>CMakeLists.txt</tt> file.
   */
  @Required
  public void setSrcRoot(String path) {
      this.srcRoot = new File(path);
  }
  /**
   * Sets the build directory into which <tt>cmake</tt> creates <tt>Makefile</tt>, and into which the
   * project is built. The attribute is required.
   *
   * @param path the path to the build directory.
   */
  @Required
  public void setBuildDir(String path) {
      this.buildDir = new File(path);
  }

  /**
   * Should the build be started from the clean location? If set to <code>true</code>, the content
   * of directory set in {@link #setBuildDir(String)} is cleaned before CMake commands are called.
   *
   * @param value <code>true</code> if CMake should start in clean directory, <code>false</code> if
   *        it can contain files being there before.
   */
  @Default(value = "false")
  public void setCleanBuild(boolean value) {
      this.cleanBuild = value;
  }

  /**
   * Sets the maximum time of the build run [in seconds] from <code>timeout=""</code> attribute. The
   * attribute is not required; if not specified, the builder can run forever ;-)
   * .
   * @param timeout time after which the build is terminated.
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Creates object into which <code><option /></code> tag will be set. Each call returns new object which is
   * expected to be set by CC. The attribute is not required.
   *
   * @return new object to configure according to the tag values.
   */
  public Object createOption() {
    options.add(new Option());
    return options.getLast();
  }
  /**
   * Adds pre-configured set of options which are merged with the current set of options set through {@link
   * #createOption()}.
   *
   * @param  optsobj the instance of {@link CMakeBuilderOptions} class
   * @throws CruiseControlException
   */
  public void   add(Object optsobj) throws CruiseControlException {
    /* Check the object type. No other are supported */
    if (!(optsobj instanceof CMakeBuilderOptions)) {
        throw new CruiseControlException("Invalid instance of options: " + optsobj.getClass().getCanonicalName());
    }
    /* Add the options to the list */
    for (Option o : ((CMakeBuilderOptions) optsobj).getOptions()) {
        options.add(o);
    }
  }

  /**
   * Creates object representing the command run after the CMake is configured; such commands are for example
   * <tt>make, make install</tt> and so on. Since the object is {@link ExecBuilder}, any command can be defined and
   * all its attributes can be set.
   *
   * Attributes not set for the command are inherited from CMake configuration.
   *
   * @return the instance of {@link ExecBuilder} representing the command to execute
   */
  @Default(value = "")
  public Object createBuild() {
      commands.add(new ExecBuilderCMake());
      return commands.getLast();
  }

  /**
   * Private wrapper for {@link #mergeEnv(OSEnvironment)} method` just calls the wrapped method. It is required
   * in order to pass env variables to the individual builders.
   */
  private void mergeEnv_wrap(@SuppressWarnings("javadoc") final OSEnvironment env) {
      mergeEnv(env);
  }



  /* ----------- ATTRIBS BLOCK ----------- */

  /** Serialization UID. */
  private static final long  serialVersionUID = -5848722491823283506L;


  /** The value set in {@link #setBuildDir(String)}. */
  private File                   buildDir;
  /** The value set in {@link #setSrcRoot(String)}. */
  private File                   srcRoot;
  /** The value set in {@link #setCleanBuild(boolean)}. */
  private boolean                cleanBuild = false;

  /** The maximum time of build run [in sec.]. */
  private long                    timeout;

  /** The list of <tt>-D</tt> defines passed to <tt>cmake</tt> command. */
  private LinkedList<Option>      options   = new LinkedList<Option>();
  /** The list of commands as they are executed one after another. */
  private LinkedList<ExecBuilder> commands  = new LinkedList<ExecBuilder>();

  /** Logger. */
  private static final Logger     LOG       = Logger.getLogger(CMakeBuilder.class);


  /* ----------- NESTED CLASSES ----------- */

  /**
   * Class for the CMake <tt>option[=value]</tt> options configuration:
   * <pre>
   *     <option value="OPTION_NAME[=OPTION_VALUE]"/>
   * </pre>
   *
   * Not that '-' must be the part of option name!
   */
  public static final class Option extends StringWriter {
     /**
      * Sets the name of the option.
      * @param option string with the define option name.
      */
     public void setValue(String option) {
       write(option);
     }
  }

  /**
   * Wrapper of {@link ExecBuilder}, calling {@link CMakeBuilder#mergeEnv(OSEnvironment)}
   */
  private final class ExecBuilderCMake extends ExecBuilder {
      /** Overrides {@link #mergeEnv(OSEnvironment)} method to call parent's
       *  {@link CMakeBuilder#mergeEnv(OSEnvironment)} first, and its own implementation then */
      @Override
      public void mergeEnv(final OSEnvironment env) {
          mergeEnv_wrap(env);
          super.mergeEnv(env);
      }
      /** Serialization UID */
      private static final long serialVersionUID = -9071669502459334465L;
  }

}
