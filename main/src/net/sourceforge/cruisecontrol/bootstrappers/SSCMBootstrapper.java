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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.SSCM;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 *  Bootstrapper for Surround SCM. Accepts one Branch/Repository path for fetching files
 *
 *  @author Matt Harp
 */
public class SSCMBootstrapper implements net.sourceforge.cruisecontrol.Bootstrapper
{
   public void validate() throws CruiseControlException { /* nothing is required */ }

   public void bootstrap() throws CruiseControlException {
      final List<SSCM.SSCMCLIParam> paramList = new ArrayList<SSCM.SSCMCLIParam>();
      final SSCM.SSCMCLIStringParam strparamFile = new SSCM.SSCMCLIStringParam("file", "", false);
      strparamFile.setData("/");
      paramList.add(strparamFile);
      paramList.add(strparamBranch);
      paramList.add(strparamRepository);
      paramList.add(fparamMakeWritable);
      paramList.add(fparamForceFetch);

      paramList.add(strparamLabel);
      if (strparamLabel.isSet()) { paramList.add(strparamIncludeRemovedFiles); }

      paramList.add(fparamRecursive);
      if (!fparamForceFetch.isSet()) { // If fetch is forced, then local file will be replaced.
         if (!strparamOverwrite.isSet()) { strparamOverwrite.setData("skip"); } // so we don't overwrite local changes.
         paramList.add(strparamOverwrite);
      }

      paramList.add(strparamServerLogin);
      paramList.add(strparamServerConnect);

      executeCLICommand(paramList);
    }

   public void setBranch(String str)              { strparamBranch.setData(str); }
   public void setRepository(String str)          { strparamRepository.setData(str); }
   public void setLabel(String str)               { strparamLabel.setData(str); }
   public void setServerConnect(String str)       { strparamServerConnect.setData(str); }
   public void setServerLogin(String str)         { strparamServerLogin.setData(str); }
   public void setIncludeRemovedFiles(boolean f) { strparamIncludeRemovedFiles.setData(f ? "" : "-"); }
   public void setOverwrite(boolean f)           { strparamOverwrite.setData(f ? "replace" : "skip"); }

    public void setRecursive(boolean f) {
        if (f) {
            fparamRecursive.setData(null);
        }
    }

    public void setForceFetch(boolean f) {
        if (f) {
            fparamForceFetch.setData(null);
        }
    }

    public void setMakeWritable(boolean f) {
        if (f) {
            fparamMakeWritable.setData(null);
        }
    }

    private final SSCM.SSCMCLIStringParam strparamBranch = new SSCM.SSCMCLIStringParam("branch", "-b", false);
    private final SSCM.SSCMCLIStringParam strparamRepository = new SSCM.SSCMCLIStringParam("repository", "-p", false);
    private final SSCM.SSCMCLIStringParam strparamLabel = new SSCM.SSCMCLIStringParam("label", "-l", false);
    private final SSCM.SSCMCLIStringParam strparamServerConnect
            = new SSCM.SSCMCLIStringParam("serverconnect", "-z", false);
    private final SSCM.SSCMCLIStringParam strparamServerLogin = new SSCM.SSCMCLIStringParam("serverlogin", "-y", false);
    private final SSCM.SSCMCLIStringParam strparamIncludeRemovedFiles
            = new SSCM.SSCMCLIStringParam("includeremoved", "-i", false);
    private final SSCM.SSCMCLIStringParam strparamOverwrite = new SSCM.SSCMCLIStringParam("overwrite", "-w", false);

    private final SSCM.SSCMCLIBoolParam fparamRecursive = new SSCM.SSCMCLIBoolParam("recursive", "-r", false);
    private final SSCM.SSCMCLIBoolParam fparamForceFetch = new SSCM.SSCMCLIBoolParam("force", "-f", false);
    private final SSCM.SSCMCLIBoolParam fparamMakeWritable = new SSCM.SSCMCLIBoolParam("writable", "-e", false);

    private static final Logger LOG = Logger.getLogger(SSCMBootstrapper.class);

    protected void executeCLICommand(final List<SSCM.SSCMCLIParam> paramList) throws CruiseControlException {
        final Commandline command = new Commandline();
        command.setExecutable("sscm");
        command.createArgument().setValue("get");

        // Next, we just iterate through the list, adding entries.
        for (final SSCM.SSCMCLIParam param : paramList) {
            if (param == null) {
                throw new IllegalArgumentException("paramList may not contain null values");
            }
            if (param.checkRequired()) {
                final String str = param.getFormatted();
                if (str != null) {
                    command.createArgument().setValue(str);
                    LOG.debug("Added cmd part: " + str);
                }
            } else {
                throw new CruiseControlException("Required parameter '" + param.getParamName() + "' is missing!");
            }
        }

        try {
            final Process process = command.execute();
            new Thread(new StreamPumper(process.getInputStream(),
                    StreamLogger.getInfoLogger(LOG))).start();
            // logs process error stream at info level
            final Thread stderr = new Thread(new StreamPumper(process.getErrorStream(),
                    StreamLogger.getInfoLogger(LOG)));
            stderr.start();

            process.waitFor();
            stderr.join();

            IO.close(process);
        } catch (IOException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        } catch (InterruptedException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        }
    }

}

