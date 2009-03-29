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
package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;

/**
 * This test references several resources from the same package.  It also
 * has "p4_client3.txt" which, though not referenced here, was used to
 * generate "p4_where3.txt".
 *
 * @author <a href="mailto:groboclown@users.sourceforge.net">Matt Albrecht</a>
 */
public class P4ChangelistLabelIncrementerTest extends TestCase {

    static class MockP4ChangelistLabelIncrementer
            extends P4ChangelistLabelIncrementer {
        public String inputText;
        public String exceptionText;
        public InputStream in;
        public Commandline cmd;

        protected void runP4Cmd(Commandline command, P4CmdParser parser)
                throws CruiseControlException {
            this.cmd = command;
            if (exceptionText != null) {
                throw new CruiseControlException(exceptionText);
            }

            if (in == null) {
                in = new ByteArrayInputStream(inputText.getBytes());
            }
            try {
                parseStream(in, parser);
            } catch (IOException e) {
                fail("Unexpected exception " + e);
            } finally {
                IO.close(in);
            }
        }
    }
    static class MockP4ChangelistLabelIncrementer2
            extends P4ChangelistLabelIncrementer {
        public Iterator in;
        public final List<Commandline> commands = new LinkedList<Commandline>();
        public MockDelete d;
        public MockFileSet fs;

        protected void runP4Cmd(Commandline cmd, P4CmdParser parser)
                throws CruiseControlException {
            this.commands.add(cmd);
            InputStream i = (InputStream) in.next();
            try {
                parseStream(i, parser);
            } catch (IOException e) {
                fail("Unexpected exception " + e);
            } finally {
                IO.close(i);
            }
        }

        protected Delete createDelete(Project p) throws CruiseControlException {
            Delete sd = super.createDelete(p);
            assertNotNull("Created null delete object", sd);
            d = new MockDelete();
            d.setProject(p);
            return d;
        }

        protected FileSet createFileSet(Project p) throws CruiseControlException {
            FileSet sfs = super.createFileSet(p);
            assertNotNull("Created null fileset object", sfs);
            fs = new MockFileSet();
            fs.setProject(p);
            return fs;
        }
    }


    public static class MockDelete extends Delete {
        public FileSet fs;
        public boolean executed = false;
        public void addFileset(FileSet fileSet) {
            assertNull("Already set the fileset", this.fs);
            this.fs = fileSet;
            super.addFileset(fileSet);
        }

        public void execute() {
            // don't do anything for real
            executed = true;
        }
    }


    public static class MockFileSet extends FileSet {
        public final List<NameEntry> excludes = new LinkedList<NameEntry>();
        public final List<NameEntry> includes = new LinkedList<NameEntry>();

        public NameEntry createExclude() {
            NameEntry ne = super.createExclude();
            excludes.add(ne);
            return ne;
        }

        public NameEntry createInclude() {
            NameEntry ne = super.createInclude();
            includes.add(ne);
            return ne;
        }
    }





    public void testValidate() {
        P4ChangelistLabelIncrementer p4 = new P4ChangelistLabelIncrementer();

        try {
            p4.validate();
            fail("P4 should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        p4.setUser("user");
        p4.setPort("port");
        p4.setClient("client");
        p4.setView("view");

        try {
            p4.validate();
        } catch (CruiseControlException e) {
            fail("P4 should not throw exceptions when required attributes are set.");
        }
    }

    public void testBuildBaseP4Command() {
        MockP4ChangelistLabelIncrementer p4 =
            new MockP4ChangelistLabelIncrementer();

        p4.setUser("x-user");
        p4.setPasswd("x-passwd");
        p4.setPort("x-port");
        p4.setClient("x-client");
        Commandline cmdLine = p4.buildBaseP4Command();

        assertEquals("p4 -s -c x-client -p x-port -u x-user -P x-passwd",
                concatCommand(cmdLine));
    }

    public void testParseChangelists1() throws Exception {
        MockP4ChangelistLabelIncrementer p4 =
            new MockP4ChangelistLabelIncrementer();

        p4.in = loadTestLog("p4_changes1.txt");

        try {
            p4.getCurrentChangelist();
            fail("Did not throw CCE");
        } catch (CruiseControlException cce) {
            if (cce.getMessage().indexOf("Could not discover the changelist") < 0) {
                fail("Wrong exception thrown");
            }
        }
    }

    public void testParseChangelists2() throws Exception {
        MockP4ChangelistLabelIncrementer p4 =
            new MockP4ChangelistLabelIncrementer();
        p4.setClient("y-client");
        p4.in = loadTestLog("p4_changes2.txt");

        String c = p4.getCurrentChangelist();
        assertEquals("Returned wrong number of changelists",
                "1138",
                c);
        assertEquals("p4 -s -c y-client changes -m1 -ssubmitted",
                concatCommand(p4.cmd));
    }

    public void testParseChangelists3() throws Exception {
        MockP4ChangelistLabelIncrementer p4 =
            new MockP4ChangelistLabelIncrementer();

        p4.in = loadTestLog("p4_changes2.txt");

        String c = p4.getCurrentChangelist();
        assertEquals("Returned wrong number of changelists",
                "1138",
                c);
        assertEquals("p4 -s changes -m1 -ssubmitted",
                concatCommand(p4.cmd));
    }

    public void testDeleteView1() throws Exception {
        final MockP4ChangelistLabelIncrementer2 p4 =
            new MockP4ChangelistLabelIncrementer2();
        p4.setView("//...");
        final List<InputStream> inp = new LinkedList<InputStream>();
        inp.add(loadTestLog("p4_where1.txt"));
        p4.in = inp.iterator();

        p4.deleteView();

        assertNotNull("Didn't create a Delete object", p4.d);
        assertNotNull("Didn't create a FileSet object", p4.fs);
        assertTrue("Didn't run the delete object", p4.d.executed);
        assertEquals("Didn't add the right number of files to fileset",
                1, p4.fs.includes.size());
        assertEquals("Incorrectly added an exclude to fileset",
                0, p4.fs.excludes.size());
        assertEquals("Didn't add the right item to fileset",
                "c:\\p4\\cc\\main" + File.separator + "**",
                p4.fs.includes.iterator().next().getName());
    }


    public void testGetWhereView1() throws Exception {
        final MockP4ChangelistLabelIncrementer2 p4 =
            new MockP4ChangelistLabelIncrementer2();
        p4.setView("//...");
        final List<InputStream> inp = new LinkedList<InputStream>();
        inp.add(loadTestLog("p4_where2.txt"));
        p4.in = inp.iterator();

        p4.getWhereView(p4.createProject());

        assertNotNull("Didn't create a FileSet object", p4.fs);
        assertEquals("Didn't add the right number of includes to fileset",
                3, p4.fs.includes.size());
        assertEquals("Didn't add the right number of excludes to fileset",
                2, p4.fs.excludes.size());
        Iterator inc = p4.fs.includes.iterator();
        assertEquals("Didn't add item 1 to include right",
                "c:\\p4-test\\cc" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        assertEquals("Didn't add item 2 to include right",
                "c:\\p4-test\\main" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        assertEquals("Didn't add item 3 to include right",
                "c:\\p4-test\\qa" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        Iterator ex = p4.fs.excludes.iterator();
        assertEquals("Didn't add item 1 to exclude right",
                "c:\\p4-test\\main\\qa" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
        assertEquals("Didn't add item 2 to exclude right",
                "c:\\p4-test\\main\\core" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
    }


    public void testGetWhereView2() throws Exception {
        final MockP4ChangelistLabelIncrementer2 p4 =
            new MockP4ChangelistLabelIncrementer2();
        p4.setView("//...");
        final List<InputStream> inp = new LinkedList<InputStream>();
        inp.add(loadTestLog("p4_where3.txt"));
        p4.in = inp.iterator();

        p4.getWhereView(p4.createProject());

        assertNotNull("Didn't create a FileSet object", p4.fs);
        assertEquals("Didn't add the right number of includes to fileset",
                3, p4.fs.includes.size());
        assertEquals("Didn't add the right number of excludes to fileset",
                4, p4.fs.excludes.size());
        Iterator inc = p4.fs.includes.iterator();
        assertEquals("Didn't add item 1 to include right",
                "c:\\p4-test\\my root\\main" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        assertEquals("Didn't add item 1 to include right",
                "c:\\p4-test\\my root\\my qa" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        assertEquals("Didn't add item 1 to include right",
                "c:\\p4-test\\my root\\a b" + File.separator + "**",
                ((NameEntry) inc.next()).getName());
        Iterator ex = p4.fs.excludes.iterator();
        assertEquals("Didn't add item 1 to exclude right",
                "c:\\p4-test\\my root\\main\\qa" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
        assertEquals("Didn't add item 1 to exclude right",
                "c:\\p4-test\\my root\\main\\a b" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
        assertEquals("Didn't add item 1 to exclude right",
                "c:\\p4-test\\my root\\main\\core" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
        assertEquals("Didn't add item 1 to exclude right",
                "c:\\p4-test\\my root\\main\\build" + File.separator + "**",
                ((NameEntry) ex.next()).getName());
    }

    public void testIsValidLabel() {
        P4ChangelistLabelIncrementer inc = new P4ChangelistLabelIncrementer();
        assertTrue(inc.isValidLabel("anything should be 'valid' and return true"));
    }

    private String concatCommand(final Commandline cmdLine) {
        final String[] args = cmdLine.getCommandline();
        final StringBuffer cmd = new StringBuffer();
        cmd.append(args[ 0 ]);
        for (int i = 1; i < args.length; i++) {
            cmd.append(" ").append(args[i]);
        }
        return new String(cmd);
    }

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(P4ChangelistLabelIncrementerTest.class);
    }

}
