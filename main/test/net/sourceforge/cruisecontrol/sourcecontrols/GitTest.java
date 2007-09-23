/*****************************************************************************
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
 *****************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * @see <a href="http://git.or.cz/">git.or.cz</a>
 * @author <a href="rschiele@gmail.com">Robert Schiele</a>
 */
public class GitTest extends TestCase {
    private static final String NEWLINE = System.getProperty("line.separator");
    private Git git;

    protected void setUp() {
        git = new Git();
    }

    public void testValidate() throws IOException {
        try {
            git.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
            // expected
        }
        
        git = new Git();
        git.setLocalWorkingCopy("invalid directory");
        try {
            git.validate();
            fail("should throw an exception when an invalid "
                 + "'localWorkingCopy' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }
        
        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();

        git = new Git();
        git.setLocalWorkingCopy(tempFile.getParent());
        try {
            git.validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when at least a valid "
                 + "'localWorkingCopy' attribute is set");
        }

        git = new Git();
        git.setLocalWorkingCopy(tempFile.getAbsolutePath());
        try {
            git.validate();
            fail("should throw an exception when 'localWorkingCopy' is "
                 + "file instead of directory.");
        } catch (CruiseControlException e) {
            // expected
        }
    }
    
    public void testGitRevision() {
        Date date = new Date(1190000000000L);
        if (Util.isWindows()) {
            assertEquals("\"@{ 1190000000}\"", Git.gitRevision(date));
        } else {
            assertEquals("@{ 1190000000}", Git.gitRevision(date));
        }
    }
    
    public void testParseLog() throws IOException {
        String gitLog = "commit 0a033e6b51bdae14c787fc915d96441c18e3a717\n"
            + "author Robert Schiele <rschiele@gmail.com> 1190000297 -0700\n"
            + "\n"
            + "    latest commit\n"
            + "    \n"
            + "    this is a multi line commit message\n"
            + "\n"
            + "diff --git a/README.txt b/README.txt\n"
            + "index 43c6998..0235ed7 100644\n"
            + "diff --git a/oldfile.txt b/oldfile.txt\n"
            + "deleted file mode 100644\n"
            + "index 43c6998..0000000\n"
            + "diff --git a/newfile.txt b/newfile.txt\n"
            + "new file mode 100644\n"
            + "index 0000000..0235ed7\n"
            + "\n"
            + "commit 1111111111111111111111111111111111111111\n"
            + "author Robert Schiele <rschiele@gmail.com> 1190000197 -0700\n"
            + "\n"
            + "    merge commit\n"
            + "\n"
            + "commit 2222222222222222222222222222222222222222\n"
            + "author Robert Schiele <rschiele@gmail.com> 1190000097 -0700\n"
            + "\n"
            + "    first commit\n"
            + "\n"
            + "diff --git a/README.txt b/README.txt\n"
            + "index 43c6998..0235ed7 100644\n";
        List mods = new ArrayList();
        SourceControlProperties props = new SourceControlProperties();
        props.assignPropertyName("hasChanges?");
        props.assignPropertyOnDeleteName("hasDeletions?");
        Git.parseLog(new StringReader(gitLog), mods, props);
        assertEquals(3, mods.size());

        Modification modref = new Modification("git");
        modref.modifiedTime = new Date(1190000297000L);
        modref.userName = "Robert Schiele";
        modref.emailAddress = "rschiele@gmail.com";
        modref.comment = "latest commit" + NEWLINE + NEWLINE
            + "this is a multi line commit message" + NEWLINE;
        modref.revision = "1190000297";
        Modification mod = (Modification) mods.get(0);
        assertEquals(modref, mod);
        List mf = mod.getModifiedFiles();
        assertEquals(3, mf.size());
        assertEquals("README.txt",
                     ((Modification.ModifiedFile) mf.get(0)).fileName);
        assertEquals("modified",
                     ((Modification.ModifiedFile) mf.get(0)).action);
        assertEquals("1190000297",
                     ((Modification.ModifiedFile) mf.get(0)).revision);
        assertEquals("oldfile.txt",
                     ((Modification.ModifiedFile) mf.get(1)).fileName);
        assertEquals("deleted",
                     ((Modification.ModifiedFile) mf.get(1)).action);
        assertEquals("1190000297",
                     ((Modification.ModifiedFile) mf.get(1)).revision);
        assertEquals("newfile.txt",
                     ((Modification.ModifiedFile) mf.get(2)).fileName);
        assertEquals("added",
                     ((Modification.ModifiedFile) mf.get(2)).action);
        assertEquals("1190000297",
                     ((Modification.ModifiedFile) mf.get(2)).revision);

        modref = new Modification("git");
        modref.modifiedTime = new Date(1190000197000L);
        modref.userName = "Robert Schiele";
        modref.emailAddress = "rschiele@gmail.com";
        modref.comment = "merge commit" + NEWLINE;
        modref.revision = "1190000197";
        mod = (Modification) mods.get(1);
        assertEquals(modref, mod);
        mf = mod.getModifiedFiles();
        assertEquals(0, mf.size());

        modref = new Modification("git");
        modref.modifiedTime = new Date(1190000097000L);
        modref.userName = "Robert Schiele";
        modref.emailAddress = "rschiele@gmail.com";
        modref.comment = "first commit" + NEWLINE;
        modref.revision = "1190000097";
        mod = (Modification) mods.get(2);
        assertEquals(modref, mod);
        mf = mod.getModifiedFiles();
        assertEquals(1, mf.size());
        assertEquals("README.txt",
                     ((Modification.ModifiedFile) mf.get(0)).fileName);
        assertEquals("modified",
                     ((Modification.ModifiedFile) mf.get(0)).action);
        assertEquals("1190000097",
                     ((Modification.ModifiedFile) mf.get(0)).revision);

        Map pm = props.getPropertiesAndReset();
        assertEquals("true", pm.get("hasChanges?"));
        assertEquals("true", pm.get("hasDeletions?"));
        assertEquals("0a033e6b51bdae14c787fc915d96441c18e3a717",
                     pm.get("gitcommitid"));
    }

    public void testParseEmptyLog() throws IOException {
        List mods = new ArrayList();
        SourceControlProperties props = new SourceControlProperties();
        props.assignPropertyName("hasChanges?");
        props.assignPropertyOnDeleteName("hasDeletions?");
        Git.parseLog(new StringReader(""), mods, props);
        assertEquals(0, mods.size());
        Map pm = props.getPropertiesAndReset();
        assertEquals(null, pm.get("hasChanges?"));
        assertEquals(null, pm.get("hasDeletions?"));
        assertEquals(null, pm.get("gitcommitid"));
    }
}
