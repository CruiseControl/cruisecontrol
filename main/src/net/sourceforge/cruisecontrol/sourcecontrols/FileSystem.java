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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * Scans a directory tree on a local drive rather than in a repository.
 *
 * @author <a href="mailto:alden@thoughtworks.com">Alden Almagro</a>
 */
public class FileSystem extends FakeUserSourceControl {

    private Hashtable properties = new Hashtable();
    private String property;

    private List modifications;
    private File folder;
    //TODO: change folder attribute to path. Can be file or directory.
    
    /**
     * Set the root folder of the directories that we are going to scan
     */
    public void setFolder(String s) {
        folder = new File(s);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(folder, "folder", this.getClass());
        ValidationHelper.assertTrue(folder.exists(),
            "folder " + folder.getAbsolutePath() + " must exist for FileSystem");
    }

    /**
     * For this case, we don't care about the quietperiod, only that
     * one user is modifying the build.
     *
     * @param lastBuild date of last build
     * @param now IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {
        modifications = new ArrayList();
        
        visit(folder, lastBuild.getTime());

        return modifications;
    }

    /**
     * Add a Modification to the list of modifications. A lot of default
     * behavior is assigned here because we don't have a repository to query the
     * modification.  All modifications will be set to type "change" and
     * userName "User".
     */
    private void addRevision(File revision) {
        Modification mod = new Modification("filesystem");

        mod.userName = getUserName();

        Modification.ModifiedFile modfile = mod.createModifiedFile(revision.getName(), revision.getParent());
        modfile.action = "change";

        mod.modifiedTime = new Date(revision.lastModified());
        mod.comment = "";
        modifications.add(mod);

        if (property != null) {
            properties.put(property, "true");
        }
    }

    /**
     * Recursively visit all files below the specified one.  Check for newer
     * timestamps
     */
    private void visit(File file, long lastBuild) {
        if ((!file.isDirectory()) && (file.lastModified() > lastBuild)) {
            addRevision(file);
        }

        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                visit(new File(file, children[i]), lastBuild);
            }
        }
    }

}