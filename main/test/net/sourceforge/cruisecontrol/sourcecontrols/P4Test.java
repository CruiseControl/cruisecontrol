/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Commandline;

/**
 *@author  Robert Watkins
 *@author  Jason Yip, jcyip@thoughtworks.com
 */
public class P4Test extends TestCase {
  
    public void testGetQuoteChar() {
        boolean windows = true;
        String quoteChar = P4.getQuoteChar(windows);
        assertEquals("\"", quoteChar);
        
        quoteChar = P4.getQuoteChar(!windows);
        assertEquals("'", quoteChar);
    }

    public void testValidate() {
        P4 p4 = new P4();

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
    
    public void testBuildChangesCommand() throws ParseException {
        P4 p4 = new P4();
        p4.setView("foo");

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = dateFormat.parse("12/30/2004");
        Commandline cmdLine = p4.buildChangesCommand(date, date, true);
        
        String[] args = cmdLine.getCommandline();
        StringBuffer cmd = new StringBuffer();
        cmd.append(args[0]);
        for (int i = 1; i < args.length; i++) {
            cmd.append(" " + args[i]);
        }
      
        assertEquals("p4 -s changes -s submitted foo@2004/12/30:00:00:00,@2004/12/30:00:00:00", cmd.toString());
    }
    
    public void testBuildChangesCommand_Unix() throws ParseException {
        P4 p4 = new P4();
        p4.setView("foo");

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = dateFormat.parse("12/30/2004");
        Commandline cmdLine = p4.buildChangesCommand(date, date, false);
      
        String[] args = cmdLine.getCommandline();
        StringBuffer cmd = new StringBuffer();
        cmd.append(args[0]);
        for (int i = 1; i < args.length; i++) {
            cmd.append(" " + args[i]);
        }
    
        assertEquals("p4 -s changes -s submitted foo@2004/12/30:00:00:00,@2004/12/30:00:00:00", cmd.toString());
    }

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    public void testParseChangelists() throws IOException {
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("p4_changes.txt"));

        P4 p4 = new P4();
        String[] changelists = p4.parseChangelistNumbers(input);
        input.close();
        assertNotNull("No changelists returned", changelists);
        assertEquals(
            "Returned wrong number of changelists",
            4,
            changelists.length);
        String[] expectedChangelists = new String[] { "14", "12", "11" };
        for (int i = 0; i < expectedChangelists.length; i++) {
            assertEquals(
                "Returned wrong changelist number",
                expectedChangelists[i],
                changelists[i]);
        }
    }

    public void testParseChangeDescriptions() throws IOException {
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("p4_describe.txt"));

        P4 p4 = new P4();
        List changelists = p4.parseChangeDescriptions(input);
        input.close();
        assertEquals(
            "Returned wrong number of changelists",
            3,
            changelists.size());

        assertEquals(
            "Wrong description",
            "Fixed support for db2. This is now the default database shipped"
                + " with HPDoc. For now that is. Still has to be tested on"
                + " PostgreSQL to see that it is still working there. The sea rch"
                + " mechanism is also upgraded to now ALMOST support AND/OR"
                + " expressions. There are thoughtsabout this, but not yet implemented (however prepared for)",
            ((Modification) changelists.get(0))
                .comment);
        assertEquals(
            "Wrong description",
            "ok, tests running smooth. Checking in mostly for backup. Not"
                + " finished yet. CIMD is comming on great and I'm starting to see a framework developing.",
            ((Modification) changelists.get(1))
                .comment);
        assertEquals(
            "Wrong description",
            "Testing ..\nSome ..\nLinebreaks.",
            ((Modification) changelists.get(2))
                .comment);
        //        XMLOutputter outputter = new XMLOutputter();
        //        for (Iterator iterator = changelistElements.iterator(); iterator.hasNext();) {
        //            Element element = (Element) iterator.next();
        //  Use next lines if you want to see the output of the run. This is what is inserted into the logs.
        //            outputter.setNewlines(true);
        //            outputter.setIndent(true);
        //            System.out.println(outputter.outputString(element));
        //        }
    }

    //    public void testGetModifications() throws Exception {
    //        
    //        // REAL TEST IF YOU NEED IT
    //        P4 p4 = new P4();
    //        p4.setView("//depot/...");
    //        List changelists = p4.getModifications(new Date(0), new Date(), 0);
    //        assertEquals("Returned wrong number of changelists", 3, changelists.size());
    //        
    //    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(P4Test.class);
    }

}
