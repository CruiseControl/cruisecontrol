/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.NonSupportedVersionControlException;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Cvs;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Perforce;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Svn;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.util.CruiseRuntime;

public class VersionControlFactory {
    private CruiseRuntime runtime;
    private static final String TYPE_PERFORCE = "perforce";
    private static final String TYPE_SVN = "svn";
    private static final String TYPE_CVS = "cvs";

    public VersionControlFactory(CruiseRuntime runtime) {
        this.runtime = runtime;
    }

    public VCS getVCSInstance(String projectName, String url, String module, String type)
            throws NonSupportedVersionControlException {
        if (TYPE_SVN.equals(type)) {
            return new Svn(url, runtime);
        }
        if (TYPE_CVS.equals(type)) {
            return new Cvs(url, module, runtime);
        }
        if (TYPE_PERFORCE.equals(type)) {
            return new Perforce(projectName, url, module, runtime);
        }
        throw new NonSupportedVersionControlException(type + " is not supported yet");
    }

}
