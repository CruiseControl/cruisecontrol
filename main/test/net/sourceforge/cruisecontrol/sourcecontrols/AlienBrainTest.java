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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Commandline;

/**
 * The unit test for an AlienBrain source control interface for
 * CruiseControl
 *
 * @author <a href="mailto:scottj+cc@escherichia.net">Scott Jacobs</a>
 */
public class AlienBrainTest extends TestCase {
   
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy z");
    private static final Date NT_TIME_ZERO;
    private static final Date JAVA_TIME_ZERO;
    
    static {
        try {
            NT_TIME_ZERO = DATE_FORMAT.parse("1/1/1601 UTC");
            JAVA_TIME_ZERO = DATE_FORMAT.parse("1/1/1970 UTC");
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Just want to see if the AlienBrain class can even be found.
     */ 
    public void testConstruction() {
        AlienBrain ab = new AlienBrain();
    }
    
    /**
     */
    public void testValidate() {
        AlienBrain ab = new AlienBrain();
        
        try {
            ab.validate();
            fail("AlienBrain should throw exceptions when required "
                + "attributes are not set.");
        } catch (CruiseControlException expected) {
        }
        
        ab.setServer("ABServer");
        ab.setDatabase("Project1");
        ab.setUser("User");
        ab.setPassword("Password");
        ab.setPath("Module1");
        
        try {
            ab.validate();
        } catch (CruiseControlException expected) {
            fail("AlienBrain should not throw exceptions when required "
                + "attributes are set.");
        }
        
    }
    
    public void testDateToFiletime() throws ParseException {
        assertEquals(0L, AlienBrain.dateToFiletime(NT_TIME_ZERO));
        assertEquals(116444736000000000L, AlienBrain.dateToFiletime(JAVA_TIME_ZERO));
        assertEquals(127610208000000000L, AlienBrain.dateToFiletime(DATE_FORMAT.parse("5/20/2005 UTC")));
    }
    
    public void testFiletimeToDate() throws ParseException {
        assertEquals(NT_TIME_ZERO, AlienBrain.filetimeToDate(0L));
        assertEquals(JAVA_TIME_ZERO, AlienBrain.filetimeToDate(116444736000000000L));
        assertEquals(DATE_FORMAT.parse("5/20/2005 UTC"), AlienBrain.filetimeToDate(127610208000000000L));
        
        Date now = new Date();
        assertEquals(now,
            AlienBrain.filetimeToDate(AlienBrain.dateToFiletime(now)));
    }
    
    public void testBuildGetModificationsCommand() throws ParseException {
        AlienBrain ab = new AlienBrain();
        
        ab.setUser("FooUser");
        ab.setPath("FooProject");

        Date date = DATE_FORMAT.parse("5/20/2005 EDT");
        Commandline cmdLine = ab.buildGetModificationsCommand(date, date);
        
        String[] args = cmdLine.getCommandline();
        StringBuffer cmd = new StringBuffer();
        cmd.append(args[0]);
        for (int ii = 1; ii < args.length; ++ii) {
            cmd.append(" " + args[ii]);
        }
        
        assertEquals("ab find FooProject -regex \"SCIT > "
            + "127610352000000000\" "
            + "-format \"#SCIT#|#DbPath#|#Changed By#|#CheckInComment#\""
            , cmd.toString());
    }
    
    public void testParseModificationDescription() throws ParseException {
        Modification m = AlienBrain.parseModificationDescription(
            "127610352000000000|/a/path/to/a/file.cpp|sjacobs|"
            + "A change that probably breaks everything.");
        
        assertEquals(DATE_FORMAT.parse("5/20/2005 EDT"), m.modifiedTime);
        assertEquals("sjacobs", m.userName);
        assertEquals("A change that probably breaks everything.", m.comment);
        //The CC AlienBrain SourceControl class does not yet support changesets.
        //therefore each modified file results in a modification containing
        //one file.
        assertEquals(1, m.files.size());
        assertEquals("/a/path/to/a/file.cpp", 
            ((Modification.ModifiedFile) (m.files.get(0))).fileName);
    }
    
    /**
     * Method taken from P4Test.java
     */
    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " 
            + getClass().getName(), testStream);
        return testStream;
    }
    
    public void testParseModifications() throws IOException, ParseException {
        BufferedInputStream is = new BufferedInputStream(loadTestLog("alienbrain_modifications.txt"));
        
        AlienBrain ab = new AlienBrain();
        
        List modifications = ab.parseModifications(is);
        is.close();
        
        assertEquals(
            "Returned wrong number of modifications.",
            7,
            modifications.size());

        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy HH:mm:ss z");
        assertEquals("Wrong modification time",
            dateFormat.parse("4/19/2005 16:51:55 EDT"),
            ((Modification) modifications.get(0)).modifiedTime);

        assertEquals("Wrong path",
            "/FooProject/Code/Vehicles/Src/Position.cpp",
            ((Modification.ModifiedFile) (((Modification) modifications.get(0)).files.get(0))).fileName);

        assertEquals("Wrong user",
            "User 1",
            ((Modification) modifications.get(0)).userName);        

        assertEquals("Wrong comment",
            "Passenger Animatoin",
            ((Modification) modifications.get(0)).comment);
            
        assertEquals("Wrong modification time",
            dateFormat.parse("5/7/2005 7:44:45 EDT"),
            ((Modification) modifications.get(6)).modifiedTime);

        assertEquals("Wrong path",
            "/FooProject/Code/Vehicles/Src/Materialnfo.cpp",
            ((Modification.ModifiedFile) (((Modification) modifications.get(6)).files.get(0))).fileName);

        assertEquals("Wrong user",
            "User 1",
            ((Modification) modifications.get(6)).userName);        

        assertEquals("Wrong comment",
            "Import from 2004",
            ((Modification) modifications.get(6)).comment);            
    }

    /**
     */    
    public void testParseNoModifications() throws IOException {
        BufferedInputStream is = 
            new BufferedInputStream(loadTestLog("alienbrain_nomodifications.txt"));
        
        AlienBrain ab = new AlienBrain();
        
        List modifications = ab.parseModifications(is);
        is.close();
        assertEquals(0, modifications.size());
    }
    
    //The following tests all actually use the AlienBrain executable and 
    //may need to access a server.  Therefore they can only be run if you 
    //have a licensed command-line client and access to a server.
/*
    private boolean disconnect() 
        throws IOException, InterruptedException {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("shutdown");
        cmdLine.createArgument().setValue("-force");
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        p.waitFor();
        //It seems that the ab command will return before the bridge 
        //process (abJXDKBridge.exe) has truly shut down.
        Thread.currentThread().sleep(2000);
        return p.exitValue() == 0;
    }
    
    public void testIsBridgeRunning() throws IOException, InterruptedException {
        disconnect();

        boolean isBridgeRunning = AlienBrain.isBridgeRunning();
        assertFalse("Bridge should not be running ", isBridgeRunning);
    }
    
    public void testIsConnected() throws IOException, InterruptedException {
        disconnect();
        
        boolean isConnected = AlienBrain.isConnected();
        assertFalse("Should not be connected ", isConnected);
    }
    
    public void testConnect() throws IOException, InterruptedException {
        disconnect();
        
        AlienBrain ab = new AlienBrain();
        ab.setServer("abhost");
        ab.setDatabase("StudioVault");
        ab.setUser("abuser");
        ab.setPassword("abpass");
        ab.connect();
        
        boolean isConnected = ab.isConnected();
        assertTrue(isConnected);
    }
    
    private String getActiveBranch() 
        throws IOException, InterruptedException {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("getactivebranch");
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
        
        p.waitFor();
        return reader.readLine();        
    }
    
    public void testSetActiveBranch() throws IOException, InterruptedException {
        AlienBrain ab = new AlienBrain();

        ab.setServer("abhost");
        ab.setDatabase("StudioVault");
        ab.setUser("abuser");
        ab.setPassword("abpass");
        ab.setPath("alienbrain://Project/Code/Vehicles/Classes");
        
        ab.connect();
        
        String branch = "Root Branch/Underdog";
        ab.setActiveBranch(branch);
        assertEquals("setActiveBranch failed!", 
            "The current active branch is: \"" + branch + "\"", 
            getActiveBranch());
    }
    
    public void testGetModifications() throws Exception {
        AlienBrain ab = new AlienBrain();

        ab.setServer("abhost");
        ab.setDatabase("StudioVault");
        ab.setUser("abuser");
        ab.setPassword("abpass");
        ab.setPath("alienbrain://Project/Code/Vehicles/Classes");
        
        List modifications = ab.getModifications(new Date(0), new Date());
        assertTrue("I would have expected the AlienBrain database "
            + "to have at least one file modified since 1970!",
            0 != modifications.size());
        
        for (java.util.Iterator it = modifications.iterator(); it.hasNext(); ) {
            Modification m = (Modification) it.next();
            System.out.println(m);
        }
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(AlienBrainTest.class);
    }
*/  // End of tests the require an actual AlienBrain installation.
}
