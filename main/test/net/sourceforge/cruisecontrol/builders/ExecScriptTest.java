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
package net.sourceforge.cruisecontrol.builders;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jdom.Element;
import org.junit.Test;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

public class ExecScriptTest {

    private static String space = " ";
    private static String quote = "'";

    @Test
    public void testGetCommandLineArgsWin() throws CruiseControlException {
        String testExecCmd = "dir";
        String[] simpleCmd = { testExecCmd };

        ExecScript script = createExecScript(testExecCmd, null);
        TestUtil.assertArray("simpleCmd", simpleCmd, script.buildCommandline().getCommandline());

        String testExecArgs = "C:\\temp";
        script = createExecScript(testExecCmd, testExecArgs);
        String[] detailedCmd = { testExecCmd, testExecArgs };
        TestUtil.assertArray("detailedCmd", detailedCmd, script.buildCommandline().getCommandline());
    }

    @Test
    public void testGetCommandLineArgsUnix() throws CruiseControlException {
        String testExecCmd = "/bin/sh";
        String testExecArg1 = "rm";
        String testExecArg2 = "rm -rf *";
        String testExecArgs = testExecArg1 + space + quote + testExecArg2 + quote;

        testExecCmd = testExecCmd.replace('/', File.separatorChar); // os-specific
        String[] testExecArr = { testExecCmd, testExecArg1, testExecArg2 };

        ExecScript script = createExecScript(testExecCmd, testExecArgs);

        TestUtil.assertArray("detailedCmd", testExecArr, script.buildCommandline().getCommandline());
    }

    @Test
    public void testGetCommandLineArgsURL() throws CruiseControlException {
        String testExecCmd = "/bin/sh";
        String testExecArg1 = "svn";
        String testExecArg2 = "checkout";
        String testExecArg3 = "https://cruisecontrol.svn.sourceforge.net/svnroot/cruisecontrol/trunk/cruisecontrol";
        String testExecArgs = testExecArg1 + space + testExecArg2 + space + quote + testExecArg3 + quote;

        testExecCmd = testExecCmd.replace('/', File.separatorChar); // os-specific
        String[] testExecArr = { testExecCmd, testExecArg1, testExecArg2, testExecArg3 };

        ExecScript script = createExecScript(testExecCmd, testExecArgs);

        TestUtil.assertArray("detailedCmd", testExecArr, script.buildCommandline().getCommandline());
    }
    
    @Test
    public void consumeLineShouldReturnMessages() {
        ExecScript script = new ExecScript();
        Element log = new Element("build");
        script.setBuildLogElement(log);
        script.consumeLine("hello world");
        List children = log.getChildren();
        assertEquals(1, children.size());

        Element child = (Element) children.get(0);
        assertEquals("message", child.getName());
        assertEquals("info", child.getAttributeValue("priority"));
        assertEquals("hello world", child.getText());
    }
    
    @Test
    public void consumeLineShouldReturnErrorMessagesWhenErrorStrSet() {
        ExecScript script = new ExecScript();
        Element log = new Element("build");
        script.setBuildLogElement(log);
        script.setErrorStr("foo");
        script.consumeLine("foo bar");
        List children = log.getChildren();
        assertEquals(1, children.size());

        Element child = (Element) children.get(0);
        assertEquals("message", child.getName());
        assertEquals("error", child.getAttributeValue("priority"));
        assertEquals("foo bar", child.getText());        
    }
    
    @Test
    public void consumeLineShouldReturnMessagesWithErrorStrSet() {
        ExecScript script = new ExecScript();
        Element log = new Element("build");
        script.setBuildLogElement(log);
        script.setErrorStr("foo");
        script.consumeLine("hello world");
        List children = log.getChildren();
        assertEquals(1, children.size());

        Element child = (Element) children.get(0);
        assertEquals("message", child.getName());
        assertEquals("info", child.getAttributeValue("priority"));
        assertEquals("hello world", child.getText());
    }

    private ExecScript createExecScript(String testExecCmd, String testExecArgs) {
        ExecScript script = new ExecScript();
        script.setErrorStr("error in compilation");
        script.setExecCommand(testExecCmd);
        script.setExecArgs(testExecArgs);

        return script;
    }
    
}
