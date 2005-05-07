/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit Copyright (c) 2001,
 * ThoughtWorks, Inc. 651 W Washington Ave. Suite 600 Chicago, IL 60661 USA All
 * rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  + Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the names of
 * its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class NantScriptTest extends TestCase {
    
    private Hashtable properties;
    private String nantCmd = "NAnt.exe";

    private NantScript script;

    protected void setUp() throws Exception {
        script = new NantScript();

        // Must be a cleaner way to do this...
//        builder.setNantWorkingDir(new File(
//                new URI(ClassLoader.getSystemResource("test.build").toString())).getParent());
        properties = new Hashtable();
        properties.put("label", "200.1.23");
        script.setBuildProperties(properties);
        script.setNantProperties(new ArrayList());
        script.setLoggerClassName(NantBuilder.DEFAULT_LOGGER);
        script.setTarget("target");
        script.setBuildFile("buildfile");

        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%m%n")));

    }

   

    public void testGetCommandLineArgs() throws CruiseControlException {
        String[] resultInfo = { nantCmd, 
                "-listener:NAnt.Core.XmlLogger", 
                "-D:XmlLogger.file=log.xml",
                "-D:label=200.1.23", 
                "-buildfile:buildfile", 
                "target" };
        TestUtil.assertArray(
                "resultInfo",
                resultInfo,
                script.getCommandLineArgs());          

        String[] resultLogger = { nantCmd, 
                "-logger:NAnt.Core.XmlLogger", 
                "-logfile:log.xml", 
                "-D:label=200.1.23",
                "-buildfile:buildfile", 
                "target" };
        script.setUseLogger(true);
        TestUtil.assertArray(
                "resultLogger",
                resultLogger,
                script.getCommandLineArgs());           
    }

    public void testGetCommandLineArgs_EmptyLogger() throws CruiseControlException {
        String[] resultInfo = { nantCmd, 
                "-listener:NAnt.Core.XmlLogger", 
                "-D:XmlLogger.file=log.xml",
                "-buildfile:buildfile", 
                "target" };
        properties.put("label", "");
        TestUtil.assertArray(
                "resultInfo",
                resultInfo,
                script.getCommandLineArgs());         

        String[] resultLogger = { nantCmd, 
                "-logger:NAnt.Core.XmlLogger", 
                "-logfile:log.xml", 
                "-buildfile:buildfile",
                "target" };
        script.setUseLogger(true);
        TestUtil.assertArray(
                "resultLogger",
                resultLogger,
                script.getCommandLineArgs());         
    }

    public void testGetCommandLineArgs_Debug() throws CruiseControlException {
        String[] resultDebug = { nantCmd, 
                "-logger:NAnt.Core.XmlLogger", 
                "-logfile:log.xml", 
                "-debug+",
                "-D:label=200.1.23", 
                "-buildfile:buildfile", 
                "target" };
        script.setUseDebug(true);
        script.setUseLogger(true);
        TestUtil.assertArray(
                "resultDebug",
                resultDebug,
                script.getCommandLineArgs());
    }

    public void testGetCommandLineArgs_Quiet() throws CruiseControlException {
        String[] resultQuiet = { nantCmd, 
                "-logger:NAnt.Core.XmlLogger", 
                "-logfile:log.xml", 
                "-quiet+",
                "-D:label=200.1.23", 
                "-buildfile:buildfile", 
                "target" };
        script.setUseQuiet(true);
        script.setUseLogger(true);
        TestUtil.assertArray(
                "resultQuiet",
                resultQuiet,
                script.getCommandLineArgs());
    }

}
