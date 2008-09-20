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
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

public class PlasticSCMTest extends TestCase {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat (PlasticSCM.DATEFORMAT);

    private PlasticSCM plasticscm;

    private final String workingDir = "plastictestdir";
    private File fileWorkingDir = new File (workingDir);

    protected void setUp() {
        plasticscm = new PlasticSCM();
        if (fileWorkingDir.exists()) {
            fileWorkingDir.delete();
        }
    }

    public void tearDown() {
        plasticscm = null;
        fileWorkingDir.delete();
    }


    public void testValidate() throws CruiseControlException {

        try {
            plasticscm.validate();
            fail("PlasticSCM should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        plasticscm = new PlasticSCM();
        plasticscm.setBranch("br:/main");
        try {
            plasticscm.validate();
            fail("PlasticSCM should throw an exception when the field wkspath is not set");
        } catch (CruiseControlException e) {
        }

        plasticscm = new PlasticSCM();
        plasticscm.setWkspath(workingDir);
        try {
            plasticscm.validate();
            fail("PlasticSCM should throw an exception when the field branch is not set");
        } catch (CruiseControlException e) {
        }

        plasticscm = new PlasticSCM();
        plasticscm.setWkspath(workingDir);
        plasticscm.setBranch("br:/main");
        try {
            plasticscm.validate();
            fail("PlasticSCM should throw an exception when wkspath not exists.");
        } catch (CruiseControlException e) {
        }

        plasticscm = new PlasticSCM();
        fileWorkingDir.mkdir();
        plasticscm.setWkspath(workingDir);
        plasticscm.setBranch("br:/main");
        plasticscm.validate();


        plasticscm = new PlasticSCM();
        plasticscm.setWkspath(workingDir);
        plasticscm.setBranch("br:/main");
        plasticscm.setRepository("mainrep");
        plasticscm.validate();
    }

    public void  testBuildFindCommand() throws CruiseControlException, ParseException {

        fileWorkingDir.mkdir();

        plasticscm.setWkspath(workingDir);
        plasticscm.setBranch("br:/main");
        Date lastBuild = dateFormat.parse("01.01.2007.00.00.01");
        Date now = dateFormat.parse("03.03.2007.22.55.43");
        String command = "cm find revision where branch = 'br:/main' "
                       + "and revno != 'CO' "
                       + "and date between '01.01.2007.00.00.01' and '03.03.2007.22.55.43' ";
        String format = "--dateformat=\"" + PlasticSCM.DATEFORMAT + "\" "
                       + "--format=\"" + PlasticSCM.QUERYFORMAT + "\"";


        assertEquals(command + format, plasticscm.buildFindCommand(lastBuild, now).toStringNoQuoting());

        plasticscm.setRepository("mainrep");

        assertEquals(command + "on repository 'mainrep' " + format,
                plasticscm.buildFindCommand(lastBuild, now).toStringNoQuoting());
    }


    /**
     * Tests the result returned by a Plastic SCM query.
     */
    public void testParseQueryResult() throws IOException, ParseException {
        InputStream testStream = getClass().getResourceAsStream("plasticscm-query.txt");
        assertNotNull("failed to load resource plasticscm-query.txt", testStream);

        fileWorkingDir.mkdir();

        File srcDir = new File(workingDir + "/src");
        File helloFile = new File(workingDir + "/src/HelloWorld.java");
        File buildFile = new File(workingDir + "/build.xml");

        if (!srcDir.exists()) {
            srcDir.mkdir();
        }

        if (!helloFile.exists()) {
            helloFile.createNewFile();
        }

        if (!buildFile.exists()) {
            buildFile.createNewFile();
        }

        List list = plasticscm.parseStream(testStream);
        assertEquals (2, list.size());

        Modification mod = new Modification ("plasticscm");
        mod.createModifiedFile("HelloWorld.java", (new File(workingDir + "/src")).getPath());
        mod.userName = "testing01";
        mod.modifiedTime = dateFormat.parse("23.05.2007.15.41.30");
        assertEquals (mod, list.get(0));

        mod = new Modification ("plasticscm");
        mod.createModifiedFile("build.xml", fileWorkingDir.getPath());
        mod.userName = "testing01";
        mod.modifiedTime = dateFormat.parse("23.05.2007.15.41.40");
        assertEquals (mod, list.get(1));

        buildFile.delete();
        helloFile.delete();
        srcDir.delete();

    }
}