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

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.testutil.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom2.Element;

/**
 * Exec builder test class.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecBuilderTest extends TestCase {

    private static final String MOCK_SUCCESS = "good exec";
    private static final String MOCK_EXIT_FAILURE = "exit failure";
    private static final String MOCK_OUTPUT_FAILURE = "output failure";
    private static final String MOCK_TIMEOUT= "timeout";
    // name of environment variable used in the test; such env variable must
    // exist when the test is started (be defined by ant)
    private static final String TEST_ENVVAR = "TESTENV";

    // Current environment
    private final OSEnvironment env = new OSEnvironment();
    // The list of files created during the test - they are deleted by {@link #tearDown()} method
    private final FilesToDelete files = new FilesToDelete();

    /** Constructor */
    public ExecBuilderTest(String name) {
        super(name);
    }

    /**
     * Creates {@link ExecBuilder} configured to run platform-independent test script. The script just
     * prints the given message to the STDOUT.
     *
     * @param scriptFile the path to file to fill with the script
     * @param message the message to print
     * @return {@link ExecBuilder} instance filler to run the script
     * @throws CruiseControlException if the script cannot be created
     */
    public static ExecBuilder createEchoExec(File scriptFile, String message) throws CruiseControlException {
        if (Util.isWindows()) {
            IO.write(scriptFile, "@rem This is a good exec.bat\n" +
                                 "@echo " + message + "\n");
        } else {
            IO.write(scriptFile, "#!/bin/sh\n" +
                                 "exec echo '" + message + "'\n");
        }
        return createExecBuilder(scriptFile);
    }
    /**
     * Creates {@link ExecBuilder} configured to run platform-independent test script. The script does
     * nothing but exits with retcode 1
     *
     * @param scriptFile the path to file to fill with the script
     * @return {@link ExecBuilder} instance filler to run the script
     * @throws CruiseControlException if the script cannot be created
     *
     */
    public static ExecBuilder createExitExec(File scriptFile) throws CruiseControlException {
        if (Util.isWindows()) {
            IO.write(scriptFile, "@rem This is a bad exec.bat\n" +
                                 "@exit 1\n");
        } else {
            IO.write(scriptFile, "#!/bin/sh\n" +
                                 "exit 1\n");
        }
        return createExecBuilder(scriptFile);
    }
    /**
     * Creates {@link ExecBuilder} configured to run platform-independent test script. The script prints to
     * STDOUT all the environment variables passed when executed.
     *
     * @param scriptFile the path to file to fill with the script
     * @return {@link ExecBuilder} instance filler to run the script
     * @throws CruiseControlException if the script cannot be created
     * @see #createExecBuilder(File)
     */
    public static ExecBuilder createEnvExec(File scriptFile) throws CruiseControlException {
        if (Util.isWindows()) {
            IO.write(scriptFile, "@rem This is a good exec.bat printing environment values\n" +
                                 "@set");
        } else {
            IO.write(scriptFile, "#!/bin/sh\n" +
                                 "exec env");
        }
        return createExecBuilder(scriptFile);
    }
    /**
     * Creates {@link ExecBuilder} configured to run platform-independent test script. The script waits
     * for the given time before it ends.
     *
     * @param scriptFile the path to file to fill with the script
     * @param runlen how long the script should run (time in seconds)
     * @return {@link ExecBuilder} instance filler to run the script
     * @throws CruiseControlException if the script cannot be created
     * @see #createExecBuilder(File)
     */
    public static ExecBuilder createSleepExec(File scriptFile, int runlen) throws CruiseControlException {
        if (Util.isWindows()) {
            //IO.write(scriptFile, "@rem This is a bat file doing something for " + runlen + " seconds\n" +
            //                     "@ping -w 1 -n "+ runlen + " 127.0.0.1"); // Silly but reliable until IPv4 disappears

            // Here we must invoke the command directly. Using a .bat file causes that when the process (the
            // .bat script) is killed (by Process.destroy()), the command it invoked continues running and the
            // whole process ends when the inner command terminates. Thus, the "kill" does not work as expected ...
            ExecBuilder eb = createExecBuilder(scriptFile);
            eb.setCommand("ping");
            eb.setArgs("-w 1 -n "+ runlen + " 127.0.0.1");
            return eb;
            //
        } else {
            IO.write(scriptFile, "#!/bin/sh\n" +
                                 "exec sleep " + runlen);
        }
        return createExecBuilder(scriptFile);
    }

    /**
     * Creates {@link ExecBuilder} filled with command executing the given script file. The method
     * fills {@link ExecBuilder#setCommand(String)} and {@link ExecBuilder#setArgs(String)}.
     *
     * @param scriptFile the path to script file to run
     * @return the builder ready to execute the given command
     */
    private static ExecBuilder createExecBuilder(File scriptFile) {
        final ExecBuilder eb = new ExecBuilder();

        if (Util.isWindows()) {
            eb.setCommand(scriptFile.getAbsolutePath());
        } else {
            File shell = new File("/bin/sh");

            assertTrue(shell.exists());
            eb.setCommand(shell.getAbsolutePath()); // Doesn't need to have script with exec flag
            eb.setArgs(scriptFile.getAbsolutePath());
        }
        return eb;
    }

    /*
     * teardown test environment
     */
    @Override
    protected void tearDown() throws Exception {
        files.delete();
        super.tearDown();
    }

    /*
     * test validation of required attributes
     */
    public void testValidate() {
        final ExecBuilder ebt = new ExecBuilder();

        // test missing "command" attribute
        try {
            ebt.validate();
            fail("ExecBuilder should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'command' is required for "+ebt.getClass().getSimpleName(), e.getMessage());
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
    public void testBuild_BuildSuccess() throws Exception {
        final File goodTestScript = files.add("ExecBuilderTest.internalTestBuild", "_goodexec.bat");
        final ExecBuilder eb = createEchoExec(goodTestScript, "output from good exec");

        internalTestBuild(MOCK_SUCCESS, eb);
    } // testBuild_BuildSuccess

    /*
     * test a buid failure - exit
     */
    public void testBuild_BuildFailure() throws Exception {
        final File exitTestScript = files.add("ExecBuilderTest.internalTestBuild", "_exitexec.bat");
        final ExecBuilder eb = createExitExec(exitTestScript);

        internalTestBuild(MOCK_EXIT_FAILURE, eb);
    } // testBuild_BuildFailure

    /*
     * test a build failure - error in output
     */
    public void testBuild_OutputFailure() throws Exception {
        final File outputTestScript = files.add("ExecBuilderTest.internalTestBuild", "_goodexec.bat");
        final ExecBuilder eb = createEchoExec(outputTestScript, "some input and then an " + MOCK_OUTPUT_FAILURE + "\n");

        internalTestBuild(MOCK_OUTPUT_FAILURE, eb);
    } // testBuild_OutputFailure

    /*
     * test a script run timeout exhaust
     */
    public void testBuild_Timeout() throws Exception {
        final File sleepTestScript = files.add("ExecBuilderTest.internalTestBuild", "_sleep.bat");
        final ExecBuilder eb = createSleepExec(sleepTestScript, 12);

        eb.setTimeout(2); // DO NOT run longer than two seconds
        final Element out = internalTestBuild(MOCK_TIMEOUT, eb);

        // Output time must not be longer than timeout with 1 sec tolerance, just for sure. Still,
        // it is much less than the 12 sec run time ...
        assertNotNull(out.getAttribute("time"));
        assertRegex("Running time too long", "0 minute\\(s\\) [23] second\\(s\\)", out.getAttributeValue("time"));
    } // testBuild_Timeout

    /*
     * test environment variables in the build - define new variable
     */
    public void testBuild_NewEnvVar() throws Exception {
        final String envnew = "TESTENV_NEW";
        final String envval = "The value of the new environment variable";

        // The variable must NOT exist in the current environment
        assertNull(env.getVariable(envnew));

        // Create the env test script
        final File envTestScript = files.add("ExecBuilderTest.internalTestBuild", "_envexec.bat");
        final ExecBuilder eb = createEnvExec(envTestScript);

        setEnv(eb, envnew, envval);
        // Script must not fail and its STDOUT must contain the new variable
        final Element logout = internalTestBuild(MOCK_SUCCESS, eb);
        assertEquals(String.format("%s=%s", envnew, envval),
                findMessage(logout, String.format("^%s.*", envnew)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - delete the variable
     */
    public void testBuild_DelEnvVar() throws Exception {
        // The variable must exist in the current environment
        assertNotNull("Value of " + TEST_ENVVAR + " environment variable is not set (if this test fails in your IDE you need to configure your test runner to set this env var with a dummy value)",
                env.getVariable(TEST_ENVVAR));

        // Create the env test script
        final File envTestScript = files.add("ExecBuilderTest.internalTestBuild", "_envexec.bat");
        final ExecBuilder eb = createEnvExec(envTestScript);

        setEnv(eb, TEST_ENVVAR, null);
        // Script must not fail and its STDOUT must not contain the new variable
        final Element logout = internalTestBuild(MOCK_SUCCESS, eb);
        assertEquals(null, findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - sets the empty value to the variable
     */
    public void testBuild_EmptyEnvVal() throws Exception {
        // The variable must exist in the current environment
        assertNotNull(env.getVariable(TEST_ENVVAR));

        // Create the env test script
        final File envTestScript = files.add("ExecBuilderTest.internalTestBuild", "_envexec.bat");
        final ExecBuilder eb = createEnvExec(envTestScript);

        setEnv(eb, TEST_ENVVAR, "");
        // Script must not fail and its STDOUT must contain the variable without value
        final Element logout = internalTestBuild(MOCK_SUCCESS, eb);
        assertEquals(String.format("%s=", TEST_ENVVAR),
                findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar

    /*
     * test environment variables in the build - adds some value to the existing value
     * of the environment variable
     */
    public void testBuild_AddEnvVal() throws Exception {
        final String envval = "/dummy/beg/path" + File.pathSeparator + "${" + TEST_ENVVAR + "}" +
                File.pathSeparator +"/dummy/end/path";

        // The variable must exist in the current environment
        assertNotNull(env.getVariable(TEST_ENVVAR));
        final String path = env.getVariable(TEST_ENVVAR);

        // Create the env test script
        final File envTestScript = files.add("ExecBuilderTest.internalTestBuild", "_envexec.bat");
        final ExecBuilder eb = createEnvExec(envTestScript);

        setEnv(eb, TEST_ENVVAR, envval);
        // Script must not fail and its STDOUT must contain the variable
        final Element logout = internalTestBuild(MOCK_SUCCESS, eb);
        assertEquals(String.format("%s=%s", TEST_ENVVAR, envval.replace("${" + TEST_ENVVAR + "}", path)),
                findMessage(logout, String.format("^%s.*", TEST_ENVVAR)));
    } // testBuild_NewEnvVar


    /*
     * execute the build and check results
     */
    private Element internalTestBuild(String statusType, ExecBuilder eb) {
        Element logElement = null;
        try {
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
            assertEquals(statusType, "none", getBuildError(logElement));
        } else if (statusType.equals(MOCK_EXIT_FAILURE)) {
            assertEquals(statusType, "return code is 1", getBuildError(logElement));
        } else if (statusType.equals(MOCK_OUTPUT_FAILURE)) {
            assertEquals(statusType, "error string found", getBuildError(logElement));
        } else if (statusType.equals(MOCK_TIMEOUT)) {
            assertEquals(statusType, "build timeout", getBuildError(logElement));
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
        assertEquals(statusType, eb.getCommand(), tk.getAttribute("name").getValue());
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
              env.setDelete(true);
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

      /**
       * Get whether there was an error written to the build log returned by
       * {@link ExecBuilder#build(Map, Progress)} or {@link ExecBuilder#build(Map, Progress, InputStream)}
       * methods.
       *
       * @param buildLogElement the build log to check for error
       * @return the error string otherwise null
       */
      public static String getBuildError(Element buildLogElement) {
          if (buildLogElement.getAttribute("error") != null) {
              return buildLogElement.getAttribute("error").getValue();
          } else {
              return "none";
          }
      } // getBuildError

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

        @Override
        public void setExecArgs(String execArgs) {
            args = execArgs;
            super.setExecArgs(execArgs);
        }

    }

} // ExecBuilderTest
