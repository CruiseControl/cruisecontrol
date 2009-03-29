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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;

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
        new AlienBrain();
    }
    
    public void testProperty() {
        AlienBrain ab = new AlienBrain() {

            protected List<Modification> getModificationsFromAlienBrain(Date lastBuild, Date now)
              throws IOException, CruiseControlException {
                List<Modification> mods = new ArrayList<Modification>();
                mods.add(new Modification("modification"));
                return mods;
            }
            
        };
        ab.setPath("path");
        ab.getModifications(new Date(), new Date());
        assertEquals(0, ab.getProperties().size());
        
        ab.setProperty("property");
        ab.getModifications(new Date(), new Date());
        Map properties = ab.getProperties();
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("property"));
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

        ab.setPath("Module1");

        try {
            ab.validate();
        } catch (CruiseControlException expected) {
            fail("AlienBrain should not throw exceptions when required "
                + "attributes are set.\n" + expected);
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

        Date date = DATE_FORMAT.parse("5/20/2005 -0400");
        ManagedCommandline cmdLine = ab.buildGetModificationsCommand(date, date);

        assertEquals("ab -u FooUser find FooProject -regex \"SCIT > "
            + "127610352000000000\" "
            + "-format \"#SCIT#|#DbPath#|#Changed By#|#CheckInComment#\""
            , cmdLine.toString());
    }

    public void testParseModificationDescription() throws ParseException {
        Modification m = AlienBrain.parseModificationDescription(
            "127610352000000000|/a/path/to/a/file.cpp|sjacobs|"
            + "A change that probably breaks everything.");

        assertEquals(DATE_FORMAT.parse("5/20/2005 -0400"), m.modifiedTime);
        assertEquals("sjacobs", m.userName);
        assertEquals("A change that probably breaks everything.", m.comment);
        //The CC AlienBrain SourceControl class does not yet support changesets.
        //therefore each modified file results in a modification containing
        //one file.
        assertEquals(1, m.files.size());
        assertEquals("/a/path/to/a/file.cpp", m.files.get(0).fileName);
    }

    /**
     * @return a file as a List of Strings, one String per line.
     * @param name log file name
     * @throws IOException if an IO error occurs
     */
    private List<String> loadTestLog(String name) throws IOException {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class "
            + getClass().getName(), testStream);

        List<String> lines = new ArrayList<String>();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(testStream));
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

    public void testParseModifications() throws IOException, ParseException {
        final List<String> results = loadTestLog("alienbrain_modifications.txt");

        AlienBrain ab = new AlienBrain();

        final List<Modification> modifications = ab.parseModifications(results);

        assertEquals(
            "Returned wrong number of modifications.",
            7,
            modifications.size());

        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy HH:mm:ss z");
        assertEquals("Wrong modification time",
            dateFormat.parse("4/19/2005 16:51:55 -0400"),
            modifications.get(0).modifiedTime);

        assertEquals("Wrong path",
            "/FooProject/Code/Vehicles/Src/Position.cpp",
            modifications.get(0).files.get(0).fileName);

        assertEquals("Wrong user",
            "User 1",
            modifications.get(0).userName);

        assertEquals("Wrong comment",
            "Passenger Animatoin",
            modifications.get(0).comment);

        assertEquals("Wrong modification time",
            dateFormat.parse("5/7/2005 7:44:45 -0400"),
            modifications.get(6).modifiedTime);

        assertEquals("Wrong path",
            "/FooProject/Code/Vehicles/Src/Materialnfo.cpp",
            modifications.get(6).files.get(0).fileName);

        assertEquals("Wrong user",
            "User 1",
            modifications.get(6).userName);

        assertEquals("Wrong comment",
            "Import from 2004",
            modifications.get(6).comment);
    }


    public void testParseNoModifications() throws IOException {
        final List<String> results = loadTestLog("alienbrain_nomodifications.txt");

        AlienBrain ab = new AlienBrain();

        final List<Modification> modifications = ab.parseModifications(results);
        assertEquals(0, modifications.size());
    }

    //The following tests all actually use the AlienBrain executable and
    //may need to access a server.  Therefore they can only be run if you
    //have a licensed command-line client and access to a server.
/*
    //In order for some of the following tests to pass, these members must
    //be assigned values valid for your AlienBrain server.
    private static final String TESTING_PATH = "alienbrain://Projects/Code/Engine/Inc";
    private static final String TESTING_BRANCH = "Root Branch/SubBranch";
    // Set any of the following to null if you do not want to
    // override any NXN_AB_* environment variables you may be using.
    private static final String TESTING_USERNAME = null; //"sjacobs";
    private static final String TESTING_PASSWORD = null; //"pass123";
    private static final String TESTING_SERVER = null; //"abserver";
    private static final String TESTING_DATABASE = null; //"StudioVault";

    public void testGetModifications() throws Exception {
        AlienBrain ab = new AlienBrain();

        ab.setServer(TESTING_SERVER);
        ab.setDatabase(TESTING_DATABASE);
        ab.setUser(TESTING_USERNAME);
        ab.setPassword(TESTING_PASSWORD);
        ab.setView(TESTING_PATH);

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
*/  // End of tests that require an actual AlienBrain installation.
}
