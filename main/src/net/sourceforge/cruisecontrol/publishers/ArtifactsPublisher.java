/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.jdom.Element;

public class ArtifactsPublisher implements Publisher {

    private Copy copier = new Copy();

    private String destDir;
    private String dir;
    private String file;

    public void setDest(String dir) {
        destDir = dir;
    }

    public void setDir(String pDir) {
        this.dir = pDir;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        Project project = new Project();
        String uniqueDir = helper.getBuildTimestamp();
        File uniqueDest = new File(destDir, uniqueDir);
        if (dir != null) {
            FileSet set = new FileSet();
            set.setDir(new File(dir));
            copier.addFileset(set);
            copier.setTodir(uniqueDest);
            copier.setProject(project);
            copier.execute();
        }
        if (file != null) {
            FileUtils utils = FileUtils.newFileUtils();
            try {
                utils.copyFile(new File(file), new File(uniqueDest, file));
            } catch (IOException e) {
                throw new CruiseControlException(e);
            }
        }

    }

    public void validate() throws CruiseControlException {
        if (destDir == null) {
            throw new CruiseControlException("'destdir' not specified in configuration file.");
        }

        if (dir == null && file == null) {
            throw new CruiseControlException("'dir' or 'file' must be specified in configuration file.");
        }
    }
}