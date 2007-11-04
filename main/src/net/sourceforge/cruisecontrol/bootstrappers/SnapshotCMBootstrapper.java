/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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
import net.sourceforge.cruisecontrol.util.Commandline;

import org.apache.log4j.Logger;

/**
 * Since we rely on our build.xml to handle updating our source code, there has always been a problem with what happens
 * when the build.xml file itself changes. Previous workarounds have included writing a wrapper build.xml that will
 * check out the "real" build.xml. This class is a substitute for that practice.
 *
 * The SnapshotCMBootstrapper will handle updating a single file from SnapshotCM before the build begins.
 *
 * Usage:
 *
 * &lt;snapshotcmbootstrapper file="" /&gt;
 *
 * @author patrick.conant@hp.com
 */
public class SnapshotCMBootstrapper implements Bootstrapper {

    /** enable logging for this class */
    private static final Logger LOG = Logger.getLogger(SnapshotCMBootstrapper.class);

    /**
     * Reference to the file to bootstrap.
     */
    private String filename;

    public void setFile(String name) {
        filename = name;
    }

    /**
     * Update the specified file.
     *
     * @throws CruiseControlException
     */
    public void bootstrap() throws CruiseControlException {
        buildUpdateCommand().executeAndWait(LOG);
    }

    public void validate() throws CruiseControlException {
        if (filename == null) {
            throw new CruiseControlException("'file' is required for SnapshotCMBootstrapper");
        }
    }

    protected Commandline buildUpdateCommand() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("wco");

        commandLine.createArguments("-fR", filename);

        return commandLine;
    }

}
