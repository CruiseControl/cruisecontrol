/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommand;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommandline;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.Runner;

/**
 * Simply runs "accurev update" to update the current workspace. Automatic keep and synctime are
 * provided as options.
 *
 * @author <a href="mailto:jason_chown@scee.net">Jason Chown</a>
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 */
public class AccurevBootstrapper implements Bootstrapper {
  private boolean verbose;
  private boolean keep;
  private boolean synctime;
  private String  workspace;
  private Runner  runner;
  private AntBuilder delegate = new AntBuilder();

  /**
   * Enables/disables verbose logging
   *
   * @param verbose
   *          if true, verbose logging is enabled
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
  /**
   * Enables/disables automatic keep
   *
   * @param keep
   *          if true, "accurev keep -m" is run on the selected workspace, to keep al modified files
   */
  public void setKeep(boolean keep) {
    this.keep = keep;
  }
  /**
   * Enables/disables automatic synctime
   *
   * @param synctime
   *          if true, "accurev synctime" is run on the selected workspace, synchronizing the
   *          server's time with the client's
   */
  public void setSynctime(boolean synctime) {
    this.synctime = synctime;
  }
  /**
   * Selects a workspace
   *
   * @param workspace
   *          the path of the workspace to work in, in the local filesystem
   */
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }
  private void runAccurev(AccurevCommandline cmd) throws CruiseControlException {
    if (runner != null) {
      cmd.setRunner(runner);
    }
    cmd.setWorkspaceLocalPath(workspace);
    cmd.setVerbose(verbose);
    cmd.run();
    cmd.assertSuccess();
  }
  /**
   * Runs the bootstrapper: updates the selected workspace. If required, it runs synctime and keep
   * before updating.
   */
  public void bootstrap() throws CruiseControlException {
    if (synctime) {
      runAccurev(AccurevCommand.SYNCTIME.create());
    }
    if (keep) {
      AccurevCommandline cmdKeep = AccurevCommand.KEEP.create();
      cmdKeep.selectModified();
      cmdKeep.setComment("CruiseControl automatic keep");
      runAccurev(cmdKeep);
    }
    runAccurev(AccurevCommand.UPDATE.create());
  }
  public void setRunner(Runner runner) {
    this.runner = runner;
  }

  public void validate() throws CruiseControlException {
      delegate.validate();
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setSaveLogDir(String)
   */
  public void setSaveLogDir(String dir) {
      delegate.setSaveLogDir(dir);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntWorkingDir(String)
   */
  public void setAntWorkingDir(String dir) {
      delegate.setAntWorkingDir(dir);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntScript(String)
   */
  public void setAntScript(String antScript) {
      delegate.setAntScript(antScript);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntHome(String)
   */
  public void setAntHome(String antHome) {
      delegate.setAntHome(antHome);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTempFile(String)
   */
  public void setTempFile(String tempFileName) {
      delegate.setTempFile(tempFileName);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTarget(String)
   */
  public void setTarget(String target) {
      delegate.setTarget(target);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setBuildFile(String)
   */
  public void setBuildFile(String buildFile) {
      delegate.setBuildFile(buildFile);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseLogger(boolean)
   */
  public void setUseLogger(boolean useLogger) {
      delegate.setUseLogger(useLogger);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createJVMArg()
   */
  public Object createJVMArg() {
      return delegate.createJVMArg();
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createProperty()
   */
  public Property createProperty() {
      return delegate.createProperty();
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseDebug(boolean)
   */
  public void setUseDebug(boolean debug) {
      delegate.setUseDebug(debug);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseQuiet(boolean)
   */
  public void setUseQuiet(boolean quiet) {
      delegate.setUseQuiet(quiet);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#getLoggerClassName()
   */
  public String getLoggerClassName() {
      return delegate.getLoggerClassName();
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setLoggerClassName(String)
   */
  public void setLoggerClassName(String string) {
      delegate.setLoggerClassName(string);
  }

  /**
   * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTimeout(long)
   */
  public void setTimeout(long timeout) {
      delegate.setTimeout(timeout);
  }
}
