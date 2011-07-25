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

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

/**
 * Exec builder test class.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecBuilderTest extends TestCase {

    private static final String MOCK_SUCCESS = "good exec";
    private static final String MOCK_EXIT_FAILURE = "exit failure";
    private static final String MOCK_OUTPUT_FAILURE = "output failure";
    // name of environment variable used in the test; such env variable must
    // exist when the test is started (be defined by ant)
    private static final String TEST_ENVVAR = "TESTENV";
    // private static final String MOCK_TIMEOUT_FAILURE = "timeout failure";
    private File goodTestScript = null;
    private File exitTestScript = null;
    private File outputTestScript = null;
    private File envTestScript = null;
    // Current environment
    private final OSEnvironment env = new OSEnvironment();


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
         // prepare "good" mock files for the testing of environment variables
         // TODO: create only when needed; actually it does not have to be created for every test,
         //       but it is done so to be compatible with other test scripts
         if (Util.isWindows()) {
             envTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_envexec.bat");
             envTestScript.deleteOnExit();
             // TODO: vypisovat promennou, ktera je na vstupu zadana jako parametr
             makeTestFile(
                 envTestScript,
                 "@rem This is a good exec.bat printing environment values\n"
                     + "@set",
                 true);
         } else {
             envTestScript = File.createTempFile("ExecBuilderTest.internalTestBuild", "_envexec.sh");
             envTestScript.deleteOnExit();
             makeTestFile(
                 envTestScript,
                 "#!/bin/sh\n"
                     + "env",
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
    public void testBuild_BuildSuccess() {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_SUCCESS, eb, goodTestScript.toString());
    } // testBuild_BuildSuccess

    /*
     * test a buid failure - exit
     */
    public void testBuild_BuildFailure() {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_EXIT_FAILURE, eb, exitTestScript.toString());
    } // testBuild_BuildFailure

    /*
     * test a build failure - error in output
     */
    public void testBuild_OutputFailure() {
        ExecBuilder eb = new ExecBuilder();
        internalTestBuild(MOCK_OUTPUT_FAILURE, eb, outputTestScript.toString());
    } // testBuild_OutputFailure

    /*
     * test environment variables in the build - define new variable
     */
    public void testBuild_NewEnvVar() {
        String envnew = "TESTENV_NEW";
        String envval = "The value of the new environment variable";
        Element logout;

        // The variable must NOT exist in the current environment
        assertNull(env.getVariable(envnew));

        ExecBuilder eb = new ExecBuilder();
        setEnv(eb, envnew, envval);
        // Script must not fail and its STDOUT must contain the new variable
        logout = internalTestBuild(MOCK_SUCCESS, eb, envTestScript.toString());
        assertEquals(String.format("%s=%s", envnew, envval),
                findMessage(logout, String.format("^%s.*", envnew)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - delete the variable
     */
    public void testBuild_DelEnvVar() {
        Element logout;

        // The variable must exist in the current environment
        assertNotNull(env.getVariable(TEST_ENVVAR));

        ExecBuilder eb = new ExecBuilder();
        setEnv(eb, TEST_ENVVAR, null);
        // Script must not fail and its STDOUT must not contain the new variable
        logout = internalTestBuild(MOCK_SUCCESS, eb, envTestScript.toString());
        assertEquals(null, findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - sets the empty value to the variable
     */
    public void testBuild_EmptyEnvVal() {
        Element logout;

        // The variable must exist in the current environment
        assertNotNull(env.getVariable(TEST_ENVVAR));

        ExecBuilder eb = new ExecBuilder();
        setEnv(eb, TEST_ENVVAR, "");
        // Script must not fail and its STDOUT must contain the variable without value
        logout = internalTestBuild(MOCK_SUCCESS, eb, envTestScript.toString());
        assertEquals(String.format("%s=", TEST_ENVVAR),
                findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - adds some value to the existing value
     * of the environment variable
     */
    public void testBuild_AddEnvVal() {
        Element logout;
        String envval = "/dummy/beg/path" + File.pathSeparator + "${" + TEST_ENVVAR + "}" + 
                File.pathSeparator +"/dummy/end/path";

        // The variable must exist in the current environment
        assertNotNull(env.getVariable(TEST_ENVVAR));
        String path = env.getVariable(TEST_ENVVAR);

        ExecBuilder eb = new ExecBuilder();
        setEnv(eb, TEST_ENVVAR, envval);
        // Script must not fail and its STDOUT must contain the variable
        logout = internalTestBuild(MOCK_SUCCESS, eb, envTestScript.toString());
        assertEquals(String.format("%s=%s", TEST_ENVVAR, envval.replace("${" + TEST_ENVVAR + "}", path)),
                findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar

    /*
     * execute the build and check results
     */
    private Element internalTestBuild(String statusType, ExecBuilder eb, String script) {
        Element logElement = null;
        try {
            eb.setCommand(script);
            if (statusType.equals(MOCK_OUTPUT_FAILURE)) {
                eb.setErrorStr(MOCK_OUTPUT_FAILURE);
            }
            eb.validate();
            logElement = eb.build(new HashMap<String, String>(), null);
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

        return logElement;
     } // internalTestBuild

      /**
       * Sets the environment variable for the given exec builder
       * @param eb the builder to set the environment variable for
       * @param name the name of the environment variable
       * @param value the new value of the environment variable, or null if to delete the
       *    variable
       */
      private void setEnv(final ExecBuilder eb, final String name, String value) {
          final Builder.EnvConf env = eb.createEnv();
          /* Set the env variable, or mark it for delete */
          env.setName(name);
          if (value != null) {
              env.setValue(value);
          }
          else {
              env.markToDelete();
          }
      }
     /**
      * Checks if the STDOUT of a script (collected to build log XML element) contains the
      * line matching the given pattern, and returns the line when found.
      * @param logOut the XML element
      * get by {@link ExecBuilder#build(java.util.Map, net.sourceforge.cruisecontrol.Progress)}.
      * @param pattern the pattern to match the STDOUT against
      * @return the line matching the pattern, or <code>null</code> if no line matched.
      */
     private String findMessage(final Element logOut, final String pattern) {
         for (final Object elem : logOut.getChild("target").getChild("task").getChildren("message")) {
             if (elem instanceof Element) {
                 if (((Element)elem).getText().matches(pattern)) {
                     return ((Element)elem).getText();
                 }
             }
         }
         return null;
     }

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

    public void testArgumentsShouldHavePropertySubstituion() throws CruiseControlException {
        final MockExecScript script = new MockExecScript();
        final ExecBuilder builder = new TestExecBuilder(script);
        builder.setCommand("cmd");
        builder.setArgs("${label}");
        builder.validate();

        final Map<String, String> buildProperties = new HashMap<String, String>();
        buildProperties.put("label", "foo");
        builder.build(buildProperties, null);

        assertEquals(script.getExecArgs(), "foo");
    }

    private class TestExecBuilder extends ExecBuilder {

        private final MockExecScript script;

        public TestExecBuilder(MockExecScript script) {
            this.script = script;
        }

        @Override
        protected ExecScript createExecScript() {
            return script;
        }

        @Override
        protected boolean runScript(final ExecScript script, final ScriptRunner scriptRunner, final String dir,
                                    final String projectName, final InputStream stdinProvider) {
            return true;
        }

    }

    private class MockExecScript extends ExecScript {

        private String args;

        public String getExecArgs() {
            return args;
        }

        public void setExecArgs(String execArgs) {
            args = execArgs;
            super.setExecArgs(execArgs);
        }

    }

} // ExecBuilderTest
