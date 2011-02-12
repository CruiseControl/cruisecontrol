/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;

@Description("Copies build products to unique destination directory based on "
        + "the build timestamp.")
public class ArtifactsPublisher implements Publisher {

    private String destDir;
    private String targetDirectory;
    private String targetFile;
    private String subdirectory;
    private boolean moveInsteadOfCopy = false;
    private boolean publishOnFailure = true;

    @Description("parent directory of actual destination directory; actual destination "
            + "directory name will be the build timestamp.")
    @Required
    public void setDest(String dir) {
        destDir = dir;
    }

    @Description("will copy all files from this directory")
    @Optional("One of \"file\" or \"dir\" is required.")
    public void setDir(String pDir) {
        targetDirectory = pDir;
    }

    @Description("will copy specified file")
    @Optional("One of \"file\" or \"dir\" is required.")
    public void setFile(String file) {
        targetFile = file;
    }

    @Description("<strong>Deprecated. Use <a href=\"#onsuccess\">&lt;onsuccess&gt;</a> and"
            + "<a href=\"#onfailure\">&lt;onfailure&gt;</a> instead.</strong><br/>"
            + "set this attribute to false to stop the publisher from running when the "
            + "build fails.")
    @Optional
    @Default("true")
    public void setPublishOnFailure(boolean shouldPublish) {
        publishOnFailure = shouldPublish;
    }

    public void publish(Element cruisecontrolLog)
            throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        if (shouldPublish(helper.isBuildSuccessful())) {
            Project project = new Project();
            String timestamp = helper.getBuildTimestamp();
            File destinationDirectory = getDestinationDirectory(timestamp);

            if (targetDirectory != null) {
                publishDirectory(project, destinationDirectory);
            }
            if (targetFile != null) {
                publishFile(destinationDirectory);
            }
        }
    }

    protected boolean shouldPublish(final boolean buildSuccessful) {
        return buildSuccessful || publishOnFailure;
    }

    File getDestinationDirectory(String timestamp) {
        String targetDir = timestamp;
        if (subdirectory != null) {
            targetDir = timestamp + File.separatorChar + subdirectory;
        }
        return new File(destDir, targetDir);
    }

    void publishFile(File uniqueDest) throws CruiseControlException {
        File file = new File(targetFile);
        if (!file.exists()) {
            throw new CruiseControlException("target file " + file.getAbsolutePath() + " does not exist");
        }
        FileUtils utils = FileUtils.getFileUtils();
        try {
            utils.copyFile(file, new File(uniqueDest, file.getName()));
                if (moveInsteadOfCopy) {
                    // utils.moveFile() should be used instead (but there's no such a method)
                    FileUtils.delete(file);            
                }

        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    void publishDirectory(Project project, File uniqueDest) throws CruiseControlException {
        File directory = new File(targetDirectory);
        if (!directory.exists()) {
            throw new CruiseControlException("target directory " + directory.getAbsolutePath() + " does not exist");
        }
        if (!directory.isDirectory()) {
            throw new CruiseControlException("target directory " + directory.getAbsolutePath() + " is not a directory");
        }
        FileSet set = new FileSet();
        set.setDir(directory);
        Copy copier = createCopier();
        copier.addFileset(set);
        copier.setTodir(uniqueDest);
        copier.setProject(project);
        try {
            copier.execute();
        } catch (Exception e) {
            throw new CruiseControlException(e);
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(destDir, "dest", this.getClass());

        ValidationHelper.assertFalse(targetDirectory == null && targetFile == null,
            "'dir' or 'file' must be specified in configuration file.");

        ValidationHelper.assertFalse(targetDirectory != null && targetFile != null,
            "only one of 'dir' or 'file' may be specified.");
    }

    @Description("subdirectory under the unique (timestamp) directory to contain artifacts")
    @Optional
    public void setSubdirectory(String subdir) {
        subdirectory = subdir;
    }

    @Description("The publisher will move files/directrories instead of copying them.")
    @Optional
    @Default("false")
    public void setMoveInsteadOfCopy(boolean moveInsteadOfCopy) {
        this.moveInsteadOfCopy = moveInsteadOfCopy;
    }

    @SkipDoc // Gendoc should not interpret this as a Copy child.
    public Copy createCopier() {
        return moveInsteadOfCopy ? new Move() : new Copy();
    }
    
}
