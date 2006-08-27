/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import java.util.Hashtable;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.jdom.Element;

public class Maven2ScriptTest extends TestCase {


    public void testConsumeLine() throws Exception {
        final Element buildLogElement = new Element("testBuild");
        final Maven2Script script = new Maven2Script(buildLogElement, null, null, null, null, null, null);

        int contentIdx = 0;
        Element currElement;

        script.consumeLine("[ERROR] FATAL ERROR");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertNotNull("fatal error not detected", buildLogElement.getAttribute("error"));
        assertEquals("FATAL ERROR detected", buildLogElement.getAttribute("error").getValue());
        assertNull(buildLogElement.getAttribute("success"));
        assertEquals("message", currElement.getName());
        assertEquals("error", currElement.getAttribute("priority").getValue());

        script.consumeLine("[ERROR] BUILD ERROR");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("BUILD ERROR detected", buildLogElement.getAttribute("error").getValue());
        assertNull("BUILD ERROR detected", buildLogElement.getAttribute("success"));
        assertEquals("message", currElement.getName());
        assertEquals("error", currElement.getAttribute("priority").getValue());

        script.consumeLine("BUILD SUCCESSFUL asdfasdf");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("message", currElement.getName());
        assertEquals("info", currElement.getAttribute("priority").getValue());
        assertEquals("BUILD SUCCESSFUL detected", buildLogElement.getAttribute("success").getValue());

        script.consumeLine("[surefire] Tests run: 17, Failures: 1, Errors: 0");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("message", currElement.getName());
        assertEquals("info", currElement.getAttribute("priority").getValue());

        script.consumeLine("[test info like //loading]");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("message", currElement.getName());
        assertEquals("info", currElement.getAttribute("priority").getValue());

        script.consumeLine("[testmavengoal:pattern]");
        script.flushCurrentElement();
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("mavengoal", currElement.getName());
        assertEquals("testmavengoal:pattern", currElement.getAttribute("name").getValue());

        script.consumeLine("[INFO] Copying artifact[jar:saxon:saxon:6.5.3] to[saxon-6.5.3.jar]");
        currElement = ((Element) buildLogElement.getContent().get(contentIdx++));
        assertEquals("message", currElement.getName());
        assertEquals("info", currElement.getAttribute("priority").getValue());
    }

    /**
     * String[] getCommandLineArgs(Map, boolean, boolean, boolean, String)
     * @throws CruiseControlException
     */
    public void testGetCommandLineArgs() throws CruiseControlException {
        Maven2Script script = getScript();

        TestUtil.assertArray(
            "NoDebug:",
            new String[] {
            CMD_MVN,
            "-B",
            "-f",
            CMD_POM,
            "-Dlabel=" + CMD_LABEL },
            script.buildCommandline().getCommandline());

        script.setMvnScript("myscript.bat");
        TestUtil.assertArray(
            "Windows:",
            new String[] {
                "myscript.bat",
                "-B",
                "-f",
                CMD_POM,
                "-Dlabel=" + CMD_LABEL },
            script.buildCommandline().getCommandline());

        script.setMvnScript(CMD_MVN);
        script.setGoalset(" clean jar");
        TestUtil.assertArray(
            "WithTarget:",
            new String[] {
                CMD_MVN,
                "-B",
                "-f",
                CMD_POM,
                "clean",
                "jar",
                "-Dlabel=" + CMD_LABEL },
            // notice the spaces in goalSet
            script.buildCommandline().getCommandline());
    }

    public void testPropsWithSpace() throws CruiseControlException {
        Maven2Script script = getScript();

        Hashtable propWithSpace = new Hashtable();
        propWithSpace.put("propertyWithSpace", "I have a space");
        script.setBuildProperties(propWithSpace);
        TestUtil.assertArray(
            "NoDebug:",
            new String[] {
            CMD_MVN,
            "-B",
            "-f",
            CMD_POM //,
            // @todo Fix Maven2Scipt to handle props w/ spaces
            //"-DpropertyWithSpace=I have a space"
            },
            script.buildCommandline().getCommandline());
    }

    private static final String CMD_MVN = "testmaven.sh";
    private static final String CMD_POM = "testproject.xml";
    private static final String CMD_LABEL = "200.1.23";

    private Maven2Script getScript() {
      Maven2Script script = new Maven2Script(null, null, null, null, null, null, null);
      // none should exist for this test
      script.setMvnScript(CMD_MVN);
      script.setPomFile(CMD_POM);

      Hashtable properties = new Hashtable();
      properties.put("label", CMD_LABEL);
      script.setBuildProperties(properties);
      return script;
    }

    public void testGetCommandLineArgsWithDebug() throws CruiseControlException {
      Logger logger = Logger.getLogger(Maven2Script.class);
      LoggerRepository loggerRepository = logger.getLoggerRepository();
      Level threshold = loggerRepository.getThreshold();
      Level level = logger.getLevel();

      loggerRepository.setThreshold(Level.ALL);
      logger.setLevel(Level.DEBUG);
      Maven2Script script = getScript();
      TestUtil.assertArray(
          "WithDebug:",
          new String[] {
              CMD_MVN,
              "-B",
              "-X",
              "-f",
              CMD_POM,
              "-Dlabel=" + CMD_LABEL },
          script.buildCommandline().getCommandline());

      loggerRepository.setThreshold(threshold);
      logger.setLevel(level);
    }
}
