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

import  org.apache.log4j.Logger;
import  org.jdom2.Attribute;
import  org.jdom2.Element;

import  net.sourceforge.cruisecontrol.Builder;
import  net.sourceforge.cruisecontrol.CruiseControlException;
import  net.sourceforge.cruisecontrol.Progress;
import  net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import  net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import  net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import  net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import  net.sourceforge.cruisecontrol.util.DateUtil;
import  net.sourceforge.cruisecontrol.util.IO;
import  net.sourceforge.cruisecontrol.util.OSEnvironment;
import  net.sourceforge.cruisecontrol.util.ValidationHelper;


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
 *      {@code
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
 *      }
 *      </pre>
 * <li> CMake can be pre-configured using {@code <plugin />} framework
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

    final File builddir = getBuildDir();
    final File srcroot = getSrcRoot();

    /* A top-level CMake file and build directory arguments are required to be defined */
    ValidationHelper.assertIsSet(builddir, "builddir", this.getClass());
    ValidationHelper.assertIsSet(srcroot, "srcroot", this.getClass());

    /* Check the existence of source directory and CMakeLists.txt file in it. */
    ValidationHelper.assertExists(srcroot, "srcroot", this.getClass());
    ValidationHelper.assertExists(new File(srcroot, "CMakeLists.txt"), "srcroot", this.getClass());
    /* There must not be a file with the name set in buildDir */
    ValidationHelper.assertFalse(builddir.isFile(), "There is file '" + builddir
            + "existing, but it is required to be build directory");

    /* Validate all the cmake defines */
    for (final Option option : getOptions()) {
         ValidationHelper.assertNotEmpty(option.toString(), "option", option.getClass());
    }

    /* Build the commands to execute. The first is "raw" cmake */
    final ExecBuilderCMake builder = createBuilder();

    builder.setCommand("cmake");
    /* Options for CMake */
    for (final Option option : getOptions()) {
         builder.addOption(option);
    }
    /* srcRoot - path to CMakeLists.txt */
    builder.addPath(srcroot);
    /* CMake is the very first */
    commands.addFirst(builder);

    /* Set inherited properties and validate individual commands */
    for (final ExecBuilder c : getBuilders()) {
         if (c.getWorkingDir() == null) {
             c.setWorkingDir(builddir.getAbsolutePath());
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
    File builddir = getBuildDir();

    /* If clean directory is required, clean it */
    if (getCleanBuild()) {
        IO.delete(builddir);
    }
    /* Creates the build directory, if it does not exist. */
    if (!builddir.exists()) {
        builddir.mkdirs();
    }

    /* Call all the commands one after another */
    for (final ExecBuilder c : getBuilders()) {
        final long timeout = getTimeout();
        final long remainTime = timeout != ScriptRunner.NO_TIMEOUT
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
   * @return path set through {@link #setSrcRoot(String)} or <code>NULL</code> if the path has not been set.
   */
  public File getSrcRoot() {
      return this.srcRoot;
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
   * @return path set through {@link #setBuildDir(String)} or <code>NULL</code> if the path has not been set.
   */
  public File getBuildDir() {
      return this.buildDir;
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
   * @return the value set through {@link #setCleanBuild(boolean)}
   */
  public boolean getCleanBuild() {
    return this.cleanBuild;
  }

  /**
   * Sets the maximum time of the build run [in seconds] from <code>timeout=""</code> attribute. The
   * attribute is not required; if not specified, the builder can run forever ;-)
   * .
   * @param timeout time after which the build is terminated.
   */
  public void setTimeout(long timeout) {
    this.timeOut = timeout;
  }
  /**
   * @return the value set through {@link #setTimeout(long)}
   */
  public long getTimeout() {
    return this.timeOut;
  }

  /**
   * Creates object into which {@code <option />} tag will be set. Each call returns new
   * object which is expected to be set by CC. The attribute is not required.
   *
   * @return new object to configure according to the tag values.
   */
  public Object createOption() {
    options.add(new Option());
    return options.getLast();
  }
  /**
   * Adds pre-configured object
   * @param obj the pre-configured object (currently only instance of {@link CMakeBuilderOptions} class)
   * @throws CruiseControlException when an instance of unexpected object type is passed
   */
  @SkipDoc
  public void   add(Object obj) throws CruiseControlException {
    /* Check the instance */
    if (obj instanceof CMakeBuilderOptions) {
      /* Add the maps to the list */
      add((CMakeBuilderOptions) obj);
      return;
    }
    /* Invalid object */
    throw new CruiseControlException("Invalid configuration object: " + obj);
  }
  /**
   * Adds pre-configured set of options which are merged with the current set of options set through {@link
   * #createOption()}.
   *
   * @param  optsobj the instance of {@link CMakeBuilderOptions} class
   */
  protected void add(CMakeBuilderOptions optsobj) {
    /* Add the options to the list */
    for (Option o : optsobj.getOptions()) {
        options.add(o);
    }
    /* Add the envs to the list */
    for (EnvConf o : optsobj.getEnvs()) {
        createEnv().copy(o);
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
  @ManualChildName("ExecBuilder")
  public ExecBuilderCMake createBuild() {
      commands.add(createBuilder());
      return commands.getLast();
  }

  /**
   * Private wrapper for {@link #mergeEnv(OSEnvironment)} method` just calls the wrapped method. It is required
   * in order to pass env variables to the individual builders.
   */
  private void mergeEnv_wrap(@SuppressWarnings("javadoc") final OSEnvironment env) {
      mergeEnv(env);
  }

  /** Creates new instance of ExecBuilder, in this case it is its {@link ExecBuilderCMake} override.
   *  @return new instance */
  protected ExecBuilderCMake createBuilder() {
    return new ExecBuilderCMake();
  }

  /** Returns iterable through builders created by {@link #createBuild()}
   *  @return iterable sequence of builders */
  protected Iterable<ExecBuilderCMake> getBuilders() {
    return commands;
  }
  /** Returns iterable through options created by {@link #createOption()}
   *  @return iterable sequence of CMake options */
  protected Iterable<Option> getOptions() {
    return options;
  }


  /* ----------- ATTRIBS BLOCK ----------- */

  /** Serialization UID. */
  private static final long  serialVersionUID = -5848722491823283506L;


  /** The value set in {@link #setBuildDir(String)}. */
  private File buildDir;
  /** The value set in {@link #setSrcRoot(String)}. */
  private File srcRoot;
  /** The value set in {@link #setCleanBuild(boolean)}. */
  private boolean cleanBuild = false;

  /** The maximum time of build run [in sec.]. */
  private long timeOut = 0;

  /** The list of <tt>-D</tt> defines passed to <tt>cmake</tt> command. */
  private final LinkedList<Option>  options   = new LinkedList<Option>();
  /** The list of commands as they are executed one after another. */
  private final LinkedList<ExecBuilderCMake> commands  = new LinkedList<ExecBuilderCMake>();

  /** Logger. */
  private static final Logger LOG = Logger.getLogger(CMakeBuilder.class);


  /* ----------- NESTED CLASSES ----------- */

  /**
   * Class for the CMake <tt>option[=value]</tt> options configuration:
   * <pre>
   * {@code
   *     <option value="OPTION_NAME[=OPTION_VALUE]"/>
   * }
   * </pre>
   *
   * Not that '-' must be the part of option name!
   */
  public static class Option extends StringWriter {
     /**
      * Sets the name of the option.
      * @param option string with the define option name.
      */
     public void setValue(String option) {
       getBuffer().setLength(0);
       append(option);
     }
  }

  /**
   * Wrapper of {@link ExecBuilder}.
   * For version 2.8 and lower it has format:
   *  <pre>
   *  {@code
   *       cmake [options] <path-to-source>
   *       cmake [options] <path-to-existing-build>
   *  }
   *  </pre>
   *
   * Also, it calls {@link CMakeBuilder#mergeEnv(OSEnvironment)}
   */
  protected class ExecBuilderCMake extends ExecBuilder {

      /** Method adding single option to the list of arguments for CMake
       *  @param opt the option to add */
      @SkipDoc
      public void addOption(Option opt) {
        addArg(opt.toString());
      }

      /** Method to set the last path argument for CMake, it is either <code>path-to-source</code> or
       *  <code>path-to-existing-build</code>.
       *  @param path the path  */
      @SkipDoc
      public void addPath(File path) {
        addArg(path.getAbsolutePath());
      }

      /** Overrides {@link #mergeEnv(OSEnvironment)} method to call parent's
       *  {@link CMakeBuilder#mergeEnv(OSEnvironment)} first, and its own implementation then
       *  @param env the environment to merge into */
      @Override
      public void mergeEnv(final OSEnvironment env) {
          mergeEnv_wrap(env);
          super.mergeEnv(env);
      }

      /** Adds single string option to the list of options, separated by space, and calls
       *  {@link ExecBuilder#setArgs(String)} to set the current argument to parent
       *  @param arg the command line option to add. */
      protected void addArg(final String arg) {
        final String args = super.getArgs();
        super.setArgs((args != null && args.length() > 0 ? args + " " : "") + arg);
      }

      /** Serialization UID */
      private static final long serialVersionUID = -9071669502459334465L;
  }

}
