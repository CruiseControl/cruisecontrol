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
package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

/**
 * Exec builder test class.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecBuilderTest extends TestCase {

    private static final String MOCK_SUCCESS = "good exec";
    private static final String MOCK_EXIT_FAILURE = "exit failure";
    private static final String MOCK_OUTPUT_FAILURE = "output failure";
    // private static final String MOCK_TIMEOUT_FAILURE = "timeout failure";
    private File goodTestScript = null;
    private File exitTestScript = null;
    private File outputTestScript = null;
    
    /*
     * default constructor
     */
    public ExecBuilderTest(String name) {
        super(name);
    } // ExecBuilderTest

    /*
     * setup test environment
     */ 
    protected void setUp() throws Exception {
         // prepare "good" mock files
         if (Util.isWindows()) {
             goodTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_goodexec.bat");
             goodTestScript.deleteOnExit();
             makeTestFile(
                 goodTestScript,
                 "@rem This is a good exec.bat\n"
                     + "@echo output from good exec\n",
                 true);
         } else {
             goodTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_goodexec.sh");
             goodTestScript.deleteOnExit();
             makeTestFile(
                 goodTestScript,
                 "#!/bin/sh\n"
                     + "\n"
                     + "echo good exec\n",
                 false);
         }
         // prepare "bad" mock files - with exit value > 0
         if (Util.isWindows()) {
             exitTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_exitexec.bat");
             exitTestScript.deleteOnExit();
             makeTestFile(
                 exitTestScript,
                 "@rem This is a bad exec.bat\n"
                     + "exit 1\n",
                 true);
         } else {
             exitTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_exitexec.sh");
             exitTestScript.deleteOnExit();
             makeTestFile(
                 exitTestScript,
                 "#!/bin/sh\n"
                     + "\n"
                     + "exit 1\n",
                 false);
         }   
         // prepare "bad" mock files - containing error string
         if (Util.isWindows()) {
             outputTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_outputexec.bat");
             outputTestScript.deleteOnExit();
             makeTestFile(
                 outputTestScript,
                 "@rem This is a bad exec.bat\n"
                     + "@echo some input and then an " + MOCK_OUTPUT_FAILURE + "\n",
                 true);
         } else {
             outputTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_outputexec.sh");
             outputTestScript.deleteOnExit();
             makeTestFile(
                 outputTestScript,
                 "#!/bin/sh\n"
                     + "\n"
                     + "echo some input and then an " + MOCK_OUTPUT_FAILURE + "\n",
                 false);
         }    
    } // setUp
    
    /*
     * test validation of required attributes 
     */
    public void testValidate() {
        ExecBuilder ebt = new ExecBuilder();

        // test missing "command" attribute
        try {
            ebt.validate();
            fail("ExecBuilder should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'command' is required for ExecBuilder", e.getMessage());
        }
        
        // test no error with all required attributes
        ebt.setCommand("dir");
        try {
            ebt.validate();
        } catch (CruiseControlException e) {
            fail("ExecBuilder should not throw an exception when the required attributes are set.");
        }
    } // testValidate
    
    /*
     * test a succesful build
     */
    public void testBuild_BuildSuccess() throws IOException {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_SUCCESS, eb, goodTestScript.toString());
    } // testBuild_BuildSuccess
    
    /*
     * test a buid failure - exit
     */
    public void testBuild_BuildFailure() throws IOException {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_EXIT_FAILURE, eb, exitTestScript.toString());
    } // testBuild_BuildFailure
    
    /*
     * test a build failure - error in output
     */
    public void testBuild_OutputFailure() throws IOException {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_OUTPUT_FAILURE, eb, outputTestScript.toString());
    } // testBuild_OutputFailure
    
    /*
     * execute the build and check results
     */
    protected void internalTestBuild(String statusType, ExecBuilder eb, String script) throws IOException {
        Element logElement = null;
        try {            
            eb.setCommand(script);
            if (statusType.equals(MOCK_OUTPUT_FAILURE)) {
                eb.setErrorStr(MOCK_OUTPUT_FAILURE);
            }
            eb.validate();
            logElement = eb.build(new Hashtable());
        } catch (CruiseControlException e) {
            e.printStackTrace();
            fail("ExecBuilder should not throw exceptions when build()-ing.");
        }
        
        // check whether there was a build error
        //System.out.println("error output = " + eb.getBuildError());
        if (statusType.equals(MOCK_SUCCESS)) {
            assertEquals(statusType, "none", eb.getBuildError());
        } else if (statusType.equals(MOCK_EXIT_FAILURE)) {
            assertEquals(statusType, "return code is 1", eb.getBuildError());
        } else if (statusType.equals(MOCK_OUTPUT_FAILURE)) {
            assertEquals(statusType, "error string found", eb.getBuildError());
        }   
        
        // check the format of the produced log
        assertNotNull(statusType, logElement);
        List targetTags = logElement.getChildren("target");
        assertNotNull(statusType, targetTags);
        assertEquals(statusType, 1, targetTags.size());
        Element te = (Element) targetTags.get(0);
        assertEquals(statusType, "exec", te.getAttribute("name").getValue());
        //System.out.println("target name = " + te.getAttribute("name").getValue());
        
        List taskTags = te.getChildren("task");
        Element tk = (Element) taskTags.get(0);
        assertEquals(statusType, script, tk.getAttribute("name").getValue());
        //System.out.println("task name = " + tk.getAttribute("name").getValue());
              
        //TODO: check for contents of messages
        //Iterator msgIterator = tk.getChildren("message").iterator();
        //while (msgIterator.hasNext()) {
        //    Element msg = (Element) msgIterator.next();
        //    System.out.println("message priority = " + msg.getAttribute("priority").getValue());
        //}
     } // internalTestBuild

      /*
       * Make a test file with specified content. Assumes the file does not exist.
       */
      private void makeTestFile(File testFile, String content, boolean onWindows) throws CruiseControlException {
          IO.write(testFile, content);
          if (!onWindows) {
              Commandline cmdline = new Commandline();
              cmdline.setExecutable("chmod");
              cmdline.createArgument("755");
              cmdline.createArgument(testFile.getAbsolutePath());
              try {
                  Process p = cmdline.execute();
                  p.waitFor();
                  assertEquals(0, p.exitValue());
              } catch (Exception e) {
                  e.printStackTrace();
                  fail("exception changing permissions on test file " + testFile.getAbsolutePath());
              }
          }
      } // makeTestFile
      
} // ExecBuilderTest
