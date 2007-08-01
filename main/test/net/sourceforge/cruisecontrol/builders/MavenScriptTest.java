/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import java.util.Hashtable;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

public class MavenScriptTest extends TestCase {

    /**
     * String[] getCommandLineArgs(Map, boolean, boolean, boolean, String)
     * @throws CruiseControlException if test fails
     */
    public void testGetCommandLineArgs() throws CruiseControlException {
        MavenScript script = getScript();

        TestUtil.assertArray(
            "NoDebug:",
            new String[] {
            "testmaven.sh",
            "-Dlabel=200.1.23",
            "-b",
            "-p",
            "testproject.xml" },
            script.buildCommandline().getCommandline());

        TestUtil.assertArray(
            "Windows:",
            new String[] {
                "testmaven.sh",
                "-Dlabel=200.1.23",
                "-b",
                "-p",
                "testproject.xml" },
            script.buildCommandline().getCommandline());

        script.setGoalset(" clean jar");

        TestUtil.assertArray(
            "WithTarget:",
            new String[] {
                "testmaven.sh",
                "-Dlabel=200.1.23",
                "-b",
                "-p",
                "testproject.xml",
                "clean",
                "jar" },
        // notice the spaces in goalSet
            script.buildCommandline().getCommandline());
    }

    private MavenScript getScript() {
      MavenScript script = new MavenScript(null);
      // none should exist for this test
      script.setMavenScript("testmaven.sh");
      script.setProjectFile("testproject.xml");

      Hashtable properties = new Hashtable();
      properties.put("label", "200.1.23");
      script.setBuildProperties(properties);
      return script;
    }

    public void testGetCommandLineArgsWithDebug() throws CruiseControlException {
      Logger logger = Logger.getLogger(MavenScript.class);
      LoggerRepository loggerRepository = logger.getLoggerRepository();
      Level threshold = loggerRepository.getThreshold();
      Level level = logger.getLevel();

      loggerRepository.setThreshold(Level.ALL);
      logger.setLevel(Level.DEBUG);
      MavenScript script = getScript();
      TestUtil.assertArray(
          "WithDebug:",
          new String[] {
              "testmaven.sh",
              "-Dlabel=200.1.23",
              "-X",
              "-b",
              "-p",
              "testproject.xml" },
          script.buildCommandline().getCommandline());

      loggerRepository.setThreshold(threshold);
      logger.setLevel(level);
    }
}
