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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
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

    private List<Modification> modifications;
    private File folder;
    //TODO: change folder attribute to path. Can be file or directory.

    /**
     * @param s the root folder of the directories that we are going to scan.
     */
    public void setFolder(final String s) {
        folder = new File(s);
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
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        modifications = new ArrayList<Modification>();

        visit(folder, lastBuild.getTime());
        
        if (!modifications.isEmpty()) {
            getSourceControlProperties().modificationFound();
        }

        return modifications;
    }

    /**
     * Add a Modification to the list of modifications. A lot of default
     * behavior is assigned here because we don't have a repository to query the
     * modification.  All modifications will be set to type "change" and
     * userName "User".
     * @param revision the file to add to the list of modifications.
     */
    private void addRevision(final File revision) {
        final Modification mod = new Modification("filesystem");

        mod.userName = getUserName();

        final Modification.ModifiedFile modfile = mod.createModifiedFile(revision.getName(), revision.getParent());
        modfile.action = "change";

        mod.modifiedTime = new Date(revision.lastModified());
        mod.comment = "";
        modifications.add(mod);
    }

    /**
     * Recursively visit all files below the specified one.  Check for newer
     * timestamps.
     * @param file the first file to visit
     * @param lastBuild the timestamp of the last build
     */
    private void visit(final File file, final long lastBuild) {
        if (file.isDirectory()) {
            final String[] children = file.list();
            for (final String child : children) {
                visit(new File(file, child), lastBuild);
            }
        }

        if (file.lastModified() > lastBuild) {
            addRevision(file);
        }
    }

}