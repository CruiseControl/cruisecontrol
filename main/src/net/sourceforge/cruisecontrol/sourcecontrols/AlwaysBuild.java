/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * Returns "true" always, so that a build will happen repeatedly.
 *
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>
 */
public class AlwaysBuild implements SourceControl {

    private Hashtable properties = new Hashtable();

    /**
     * Unsupported by AlwaysBuild.
     */
    public void setProperty(String property) {

    }

    /**
     * Unsupported by AlwaysBuild.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {

    }

    /**
     * For this case, we don't care about the quietperiod, only that
     * one user is modifying the build.
     *
     * @param lastBuild date of last build
     * @param now       IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {
        List modifications = new ArrayList();

        Modification mod = new Modification();

        mod.type = "change";

        // TODO: add attribute to specify user name for modifications

        mod.userName = "User";
        mod.fileName = "force build";
        mod.folderName = "force build";
        mod.modifiedTime = new Date((new Date()).getTime() - 100000);
        mod.comment = "";
        modifications.add(mod);
        return modifications;
    }


}