/********************************************************************************
 *
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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.SysUtilMock;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.StdoutBufferTest;
import net.sourceforge.cruisecontrol.util.Util;


/**
 * The test case of {@link PipedExecBuilder} class.
 */
public final class PipedExecBuilderTest extends TestCase {

    /** The characters allowed in the texts passed through the piped commands. */
    private static final String LETTERS = "1234567890 abcdefghi jklmnopqr stuvwxz .,;?!";

    /** Set it to <code>true</code>to switch debug mode on. In the debug mode, the commands
     *  executed will print detailed information to theirs STDERR. */
    private static final boolean debugMode = false;


    /** The list of files created during the test - they are deleted by {@link #tearDown()}
     *  method ... */
    private FilesToDelete files;


    /**
     * Generates temporary file and stores it into {@link #files} array. The file is deleted
     * by {@link #tearDown()} method.
     * The method must be called after {@link #files} attribute is initialized!
     * @return a new temp file
     * @throws IOException when the file cannot be created
     */
    private File getFile() throws IOException {
        File file;

        file = File.createTempFile(this.getClass().getName(), ".txt");
        file.deleteOnExit();

        files.add(file);
        return file;
    }

    /**
     * Generates text file filled by the given number of lines with random content and store it into
     * <code>randomFile</code> provided. The same content, but sorted and unique store into
     * <code>sortedFile</code>.
     *
     * @param  randomFile the file to store the random content
     * @param  sortedFile the file to store the sorted and "uniqued" random content
     * @param  numlines the number of lines in the random file
     * @throws IOException when the file cannot be created
     */
    private void createFiles(File randomFile, File sortedFile, int numlines) throws IOException {
        OutputStream out;

        /* Reduce the numbed of lines in the debug mode ... */
        if (debugMode && numlines > 20) {
            numlines = 20;
        }

        /* Prepare the input and output files (make output unique). Trim the lines */
        List<String> randomLines = StdoutBufferTest.generateTexts(LETTERS, numlines / 4, true);
        List<String> sortedLines = new ArrayList<String>(new HashSet<String>(randomLines));

        /* Sort the output and store it to required output file */
        Collections.sort(sortedLines);
        /* Repeat the lines 2 times to get the required number of lines */
        randomLines.addAll(new ArrayList<String>(randomLines));
        randomLines.addAll(new ArrayList<String>(randomLines));

        /* Store the random file */
        out = new BufferedOutputStream(new FileOutputStream(randomFile));
        for (String l : randomLines) {
            out.write((l + "\n").getBytes());
        }
        out.close();

        /* And the sorted */
        out = new BufferedOutputStream(new FileOutputStream(sortedFile));
        for (String l : sortedLines) {
            out.write((l + "\n").getBytes());
        }
        /* Close and return the file */
        out.close();
    }

    /**
     * Setup test environment.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        /* Prepare new array of files */
        files = new FilesToDelete();
    }
    /**
     * Clears test environment.
     */
    @Override
    protected void tearDown() throws Exception {
        /* Delete the files */
        files.delete();
        super.tearDown();
    }


    /**
     * Checks the validation when ID of the program is not set (ID is required item). The pipe
     * looks like:
     * <pre>
     *  01 +-> !!??
     *     +-> 03
     * </pre>
     */
    public void testValidate_noID() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat",  "ZERO");
        setExec(builder.createExec(), null, "cat",   null,   "01"); // corrupted
        setExec(builder.createExec(), "03", "cat",  ">NULL", "01");

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when required ID attribute is not set.");

        } catch (CruiseControlException e) {
            assertEquals("exception message when the required ID attribute is not set",
                    "'ID' is required for PipedExecBuilder$Script", e.getMessage());
        }
    }

    /**
     * Checks the validation when ID of the program is not unique (unique ID is required). The
     * pipe looks like:
     * <pre>
     *  01 --> 02??!!
     * </pre>
     */
    public void testValidate_noUniqueID() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat", "ZERO");
        setExec(builder.createExec(), "02", "cat",  null,   "01");
        setExec(builder.createExec(), "02", "cat", ">NULL", "01"); // corrupted

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when IDs are not unique.");

        } catch (CruiseControlException e) {
            assertEquals("exception message when IDs are not unique",
                    "ID 02 is not unique", e.getMessage());
        }
    }

    /**
     * Checks the validation when a script is piped from not existing ID. The pipe
     * looks like:
     * <pre>
     *  01 -- ??!! --> 02 --> 03
     * </pre>
     */
    public void testValidate_invalidPipeFrom() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat", "ZERO");
        setExec(builder.createExec(), "02", "cat",  null,   "xx"); // corrupted
        setExec(builder.createExec(), "03", "cat", ">NULL", "02");

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when piped from unexisting ID.");

        } catch (CruiseControlException e) {
            assertEquals("exception message when piped from unexisting ID",
                    "Script 02 is piped from non-existing script xx", e.getMessage());
        }
    }

    /**
     * Checks the validation when a script should wait for not existing ID. The pipe
     * looks like:
     * <pre>
     *  01 -- ??!! --> 02 --> 03
     * </pre>
     */
    public void testValidate_invalidWaitFor() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat", "ZERO");
        setExec(builder.createExec(), "02", "cat",  null,   "01");
        setExec(builder.createExec(), "03", "cat", ">NULL", "02");
        /**/
        setExec(builder.createExec(), "10", "cat", "ZERO",  null, "xx"); // corrupted
        setExec(builder.createExec(), "11", "cat", ">NULL", "10");

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when waiting for unexisting ID.");

        } catch (CruiseControlException e) {
            assertEquals("exception message when waiting for unexisting ID",
                    "Script 10 waits for non-existing script xx", e.getMessage());
        }
    }

    /**
     * Checks the validation when a loop exists in piped commands. The pipe looks like:
     * <pre>
     *  01 --> 02 --> 03 --> 04 --> 05
     *            +-> 10 --> 11 --> 12 +-> 13
     *            |                    +-> 20 --+
     *            +-<------<------<------<------+
     * </pre>
     */
    public void testValidate_pipeLoop() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat", "ZERO");
        setExec(builder.createExec(), "02", "cat",  null,   "01");
        setExec(builder.createExec(), "03", "cat",  null,   "02");
        setExec(builder.createExec(), "04", "cat",  null,   "03");
        setExec(builder.createExec(), "05", "cat", ">NULL", "04");
        /**/
        setExec(builder.createExec(), "10", "cat", "ZERO",  "20"); // piped from 20
        setExec(builder.createExec(), "11", "cat",  null,   "10");
        setExec(builder.createExec(), "12", "cat",  null,   "11");
        setExec(builder.createExec(), "13", "cat", ">NULL", "12");
        /**/
        setExec(builder.createExec(), "20", "cat",  null,   "12"); // loop start

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when the pipe loop is detected.");

        } catch (CruiseControlException e) {
            assertRegex("exception message when pipe loop is detected",
                    "Loop detected, ID \\d\\d is within loop", e.getMessage());
        }
    }

    /**
     * Checks the validation when a loop exists in waiting. The pipe looks like:
     * <pre>
     *  01 --> 02(wait for 20) +-> 03 --> 04 --> 05
     *                         +-> 10 --> 11 --> 12 +-> 13
     *                                              +-> 20
     * </pre>
     */
    public void testValidate_waitLoop() {
        PipedExecBuilder builder  = new PipedExecBuilder();

        setExec(builder.createExec(), "01", "cat", "ZERO");
        // wait for 20 which depends on output of 02
        setExec(builder.createExec(), "02", "cat",  null,   "01", "20");
        setExec(builder.createExec(), "03", "cat",  null,   "02");
        setExec(builder.createExec(), "04", "cat",  null,   "03");
        setExec(builder.createExec(), "05", "cat", ">NULL", "04");
        /**/
        setExec(builder.createExec(), "10", "cat",  null,   "02");
        setExec(builder.createExec(), "11", "cat",  null,   "10");
        setExec(builder.createExec(), "12", "cat",  null,   "11");
        setExec(builder.createExec(), "13", "cat", ">NULL", "12");
        /**/
        setExec(builder.createExec(), "20", "cat",  null,   "12");

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when the wait loop is detected.");

        } catch (CruiseControlException e) {
            assertRegex("exception message when wait loop is detected",
                    "Loop detected, ID \\d\\d is within loop", e.getMessage());
        }
    }


    /**
     * Checks the correct function of the commands piping.
     * <b>This is fundamental test, actually!</b> if this test is not passed, the others will
     * fail as well.
     *
     * It simulates the following pipe: <code>cat ifile.txt | cat | cat > ofile.txt</code>.
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testScript_pipe() throws IOException, CruiseControlException {
        PipedExecBuilder builder = new PipedExecBuilder();
        File inpFile = getFile();
        File tmpFile = getFile();

        /* Create the content */
        createFiles(inpFile, tmpFile, 800);

        /* First cat - simulated by stream reader */
        InputStream cat1 = new BufferedInputStream(new FileInputStream(inpFile));

        /* Second cat - real command without arguments */
        PipedExecBuilder.Script cat2 = (PipedExecBuilder.Script) builder.createExec();
        cat2.initialize();
        cat2.setCommand("cat");
        cat2.setStdinProvider(cat1);
        cat2.setBuildProperties(new HashMap<String, String>());
        cat2.setBuildLogParent(new Element("build"));
        cat2.setBinaryStdout(false);
        cat2.setGZipStdout(false);
        /* Validate and run (not as thread here) */
        cat2.validate();
        cat2.run();

        /* Test the output - it must be the same as the input */
        assertStreams(new FileInputStream(inpFile), cat2.getStdOutReader());

        /* Third cat - real command without arguments */
        PipedExecBuilder.Script cat3 = (PipedExecBuilder.Script) builder.createExec();
        cat3.initialize();
        cat3.setCommand("cat");
        cat3.setStdinProvider(cat2.getStdOutReader());
        cat3.setBuildProperties(new HashMap<String, String>());
        cat3.setBuildLogParent(new Element("build"));
        cat3.setBinaryStdout(false);
        cat3.setGZipStdout(false);
        /* Validate and run (not as thread here) */
        cat3.validate();
        cat3.run();

        /* Test the output - it must be the same as the input */
        assertStreams(new FileInputStream(inpFile), cat3.getStdOutReader());
    }

    /**
     * Checks the correct function of the whole builder. The pipe is quite complex here:
     * <pre>
     *  31 --> 32 --> 33(add)  +-> 34(shuf) --> 35 --> 36(del) +-> 37
     *                         +-> 35                          +-> 38(sort) --> 39(uniq) --> 40
     *
     *  11 --> 12(+ out of 35) --> 13(shuf) +-> 14(grep) --> 15(del) --------> 16(sort) --> 17(uniq) --> 21
     *                                      +-> 18(grep) --> 19(sort+uniq) --> 20
     *
     *  01(out of 37) --> 02(sort) --> 03(uniq) --> 04
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testBuild_correct() throws IOException, CruiseControlException {

        // @todo Fix on windows.
        if (Util.isWindows()) {
            System.out.println("skipping test: " + getName() + "; Need to fix on Windows.");
            return;
        }

        PipedExecBuilder builder   = new PipedExecBuilder();
        Element buildLog;

        /* Input and result files */
        File inp1File = getFile();
        File inp2File = getFile();
        File res1File = getFile();
        File res2File = getFile();
        /* Prepare content */
        createFiles(inp1File, res1File, 200);
        createFiles(inp2File, res2File, 500);

        /* Temporary and output files */
        File tmp1File = getFile();
        File tmp2File = getFile();
        File out1File = getFile();
        File out2File = getFile();
        File out3File = getFile();
        File out4File = getFile();


        /* Fill it by commands to run*/
        builder.setShowProgress(false);
        builder.setTimeout(180);
        /* Set commands */
        setExec(builder.createExec(), "01", "cat",      tmp2File.getAbsolutePath(),        null, "37");
        setExec(builder.createExec(), "02", "sort",     null,                              "01");
        setExec(builder.createExec(), "03", "uniq",     null,                              "02");
        setExec(builder.createExec(), "04", "cat",  ">"+out4File.getAbsolutePath(),        "03");
        /**/
        setExec(builder.createExec(), "11", "cat",      inp2File.getAbsolutePath());
        setExec(builder.createExec(), "12", "cat",      tmp1File.getAbsolutePath() + " -", "11", "35");
        setExec(builder.createExec(), "13", "shuf",     null,                              "12");
        setExec(builder.createExec(), "14", "grep",     "'^\\s*([0-9]+\\s+){3}'",          "13");
        setExec(builder.createExec(), "15", "del",      null,                              "14");
        setExec(builder.createExec(), "16", "sort",     null,                              "15");
        setExec(builder.createExec(), "17", "uniq",     null,                              "16");
        setExec(builder.createExec(), "18", "grep",     "-v '^\\s*([0-9]+\\s+){3}'",       "13");
        setExec(builder.createExec(), "19", "sort",     "-u",                              "18");
        setExec(builder.createExec(), "20", "cat",  ">"+out2File.getAbsolutePath(),        "19");
        setExec(builder.createExec(), "21", "cat",  ">"+out3File.getAbsolutePath(),        "17");
        /**/
        setExec(builder.createExec(), "31", "cat",      inp1File.getAbsolutePath());
        setExec(builder.createExec(), "32", "cat",      null,                              "31");
        setExec(builder.createExec(), "33", "add",      null,                              "32");
        setExec(builder.createExec(), "34", "shuf",     null,                              "33");
        setExec(builder.createExec(), "35", "cat",  ">"+tmp1File.getAbsolutePath(),        "33");
        setExec(builder.createExec(), "36", "del",      null,                              "34");
        setExec(builder.createExec(), "37", "cat",  ">"+tmp2File.getAbsolutePath(),        "36");
        setExec(builder.createExec(), "38", "sort",     null,                              "36");
        setExec(builder.createExec(), "39", "uniq",     null,                              "38");
        setExec(builder.createExec(), "40", "cat",  ">"+out1File.getAbsolutePath(),        "39");

        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);

        printXML(buildLog);
        /* No 'error' attribute must exist in the build log */
        assertNull("error attribute was found in build log!", buildLog.getAttribute("error"));
        /* And finally compare the files */
        assertFiles(res1File, out1File);
        assertFiles(res1File, out3File);
        assertFiles(res1File, out4File);
        assertFiles(res2File, out2File);

        /* And once again! */
        buildLog = builder.build(new HashMap<String, String>(), null);

        printXML(buildLog);
        /* No 'error' attribute must exist in the build log */
        assertNull("error attribute was found in build log!", buildLog.getAttribute("error"));
        /* And finally compare the files */
        assertFiles(res1File, out1File);
        assertFiles(res1File, out3File);
        assertFiles(res1File, out4File);
        assertFiles(res2File, out2File);

    }

    /**
     * Checks the function of the whole builder when a command is not found. The pipe looks like:
     * <pre>
     *  01 --> 02 +-> 03(invalid)
     *            +-> 04 --> 05
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testBuild_badCommand() throws IOException, CruiseControlException {
        PipedExecBuilder builder  = new PipedExecBuilder();
        Element buildLog;
        Attribute error;

        /* Fill it by commands to run */
        builder.setShowProgress(false);
        builder.setTimeout(300000);

        /* Output and "somewhere-in-the middle" temporary file, input and corresponding result
         * files */
        File out1File = getFile();
        File tmp1File = getFile();
        File inp1File = getFile();
        File res1File = getFile();
        /* Prepare content */
        createFiles(inp1File, res1File, 200);


        /* Set commands */
        setExec(builder.createExec(), "01", "cat",      inp1File.getAbsolutePath());
        setExec(builder.createExec(), "02", "shuf",                                 "01");
        /* corrupted: bad binary name */
        setExec(builder.createExec(), "03", "BAD",      tmp1File.getAbsolutePath(), "02");
        setExec(builder.createExec(), "04", "sort", "-u",                           "02");
        setExec(builder.createExec(), "05", "cat",  ">"+out1File.getAbsolutePath(), "04");


        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);
        error    = buildLog.getAttribute("error");

        printXML(buildLog);
        /* 'error' attribute must exist in the build log */
        assertNotNull("error attribute was not found in build log!", error);
        assertRegex("unexpected error message is returned", "(exec error.*|return code .*)", error.getValue());
    }


    /**
     * Checks the function of the whole builder when an option of a command is invalid. The pipe
     * looks like:
     * <pre>
     *  01 +-> 02(bad option)
     *     +-> 03
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testBuild_badOption() throws IOException, CruiseControlException {
        PipedExecBuilder builder  = new PipedExecBuilder();
        Element buildLog;
        Attribute error;

        /* Output file, input and corresponding result files */
        File out1File = getFile();
        File inp1File = getFile();
        File res1File = getFile();
        /* Prepare content */
        createFiles(inp1File, res1File, 200);

        /* Fill it by commands to run. */
        builder.setShowProgress(false);
        builder.setTimeout(180);

        /* Set commands */
        setExec(builder.createExec(), "01", "cat",      inp1File.getAbsolutePath());
        /* corrupted: unknown option */
        setExec(builder.createExec(), "02", "cat", ">/////",                       "01");
        setExec(builder.createExec(), "03", "cat", ">"+out1File.getAbsolutePath(), "01"); // OK


        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);
        error    = buildLog.getAttribute("error");

        printXML(buildLog);
        /* 'error' attribute must exist in the build log */
        assertNotNull("error attribute was not found in build log!", error);
        assertRegex("unexpected error message is returned", "return code .*", error.getValue());
    }


    /**
     * Checks the timeout function. The pipe looks like:
     * <pre>
     *  01(infinite read) --> 02 --> 03
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testBuild_timeout() throws CruiseControlException, IOException {

        // @todo Fix on windows.
        if (Util.isWindows()) {
            System.out.println("skipping test: " + getName() + "; Need to fix on Windows.");
            return;
        }

        PipedExecBuilder builder = new PipedExecBuilder();
        Element buildLog;
        Attribute error;

        /* Output files, input and corresponding result files */
        File out1File = getFile();
        File out2File = getFile();
        File inp1File = getFile();
        File res1File = getFile();
        /* Prepare content */
        createFiles(inp1File, res1File, 200);

        /* Fill it by commands to run. */
        builder.setTimeout(10);
        builder.setShowProgress(false);

        /* Read from infinite file (with some waiting not to read large amoutn of data ...
         * Option also is to read from STDIN, but not to have the command piped from
         * another command; however, it is not going to be worked, as STDIN of the command is
         * closed as soon as nothing can be read from. See also #testBuild_stdinClose() */
        setExec(builder.createExec(), "01", "cat",  "-D 1000 ZERO");
        setExec(builder.createExec(), "02", "sort", "-u",                           "01");
        setExec(builder.createExec(), "03", "cat",  ">"+out2File.getAbsolutePath(), "02");

        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);
        error    = buildLog.getAttribute("error");

        printXML(buildLog);
        /* And finally compare the files */
        assertFiles(out1File, out1File);
        /* 'error' attribute must exist in the build log, ant it must hold 'timeout' string */
        assertNotNull("error attribute was not found in build log!", error);
        assertRegex("unexpected error message is returned", "build timeout.*", error.getValue());
    }

    /**
     * Checks if the STDIN of a command is closed when it is not piped from another command.
     * When STDIO is not closed <code>cat</code> command should wait for infinite time.
     * The pipe looks like:
     * <pre>
     *  01(no input) --> 02 --> 03
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testBuild_stdinClose() throws CruiseControlException, IOException {

        // @todo Fix on windows.
        if (Util.isWindows()) {
            System.out.println("skipping test: " + getName() + "; Need to fix on Windows.");
            return;
        }

        PipedExecBuilder builder = new PipedExecBuilder();
        Element buildLog;
        long startTime = System.currentTimeMillis();

        /* Set 2 minutes long timeout. */
        builder.setTimeout(120);
        builder.setShowProgress(false);

        /* Read from stdin, but do not have it piped. The command must end immediately
         * without error, as the stdin MUST BE closed when determined that nothing can be
         * read from */
        setExec(builder.createExec(), "01", "cat",     null);
        setExec(builder.createExec(), "02", "cat", ">"+getFile().getAbsolutePath(), "01");

        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);

        printXML(buildLog);
        /* First of all, the command must not run. 20 seconds must be far enough! */
        assertTrue("command run far longer than it should!", (System.currentTimeMillis() - startTime) / 1000 < 20);
        /* No 'error' attribute must exist in the build log */
        assertNull("error attribute was found in build log!", buildLog.getAttribute("error"));
    }


    /**
     * Method filling the {@link PipedExecBuilder.Script} class no piped from another scripts,
     * without working dir and not waiting for another script - {@link PipedExecBuilder.Script#setPipeFrom(String)},
     * {@link PipedExecBuilder.Script#setWaitFor(String)} and
     * {@link PipedExecBuilder.Script#setWorkingDir(String)} are not called.
     *
     * @param exec the instance to fill, <b>must not</b> be <code>null</code>.
     * @param id {@link PipedExecBuilder.Script#setID(String)}, may be <code>null</code>
     * @param command {@link PipedExecBuilder.Script#setCommand(String)}, may be <code>null</code>
     *        Note that the command must be supported by {@link SysUtilMock} class!
     * @param args {@link PipedExecBuilder.Script#setArgs(String)}, may be <code>null</code>
     */
    private static void setExec(Object exec, String id, String command, String args) {
        setExec(exec, id, command, args, null, null, null);
    }
    /**
     * Method filling the {@link PipedExecBuilder.Script} class without working dir and not
     * waiting for another script - {@link PipedExecBuilder.Script#setWaitFor(String)} and
     * {@link PipedExecBuilder.Script#setWorkingDir(String)} are not called.
     *
     * @param exec the instance to fill, <b>must not</b> be <code>null</code>.
     * @param id {@link PipedExecBuilder.Script#setID(String)}, may be <code>null</code>
     * @param command {@link PipedExecBuilder.Script#setCommand(String)}, may be <code>null</code>
     *        Note that the command must be supported by {@link SysUtilMock} class!
     * @param args {@link PipedExecBuilder.Script#setArgs(String)}, may be <code>null</code>
     * @param pipeFrom {@link PipedExecBuilder.Script#setPipeFrom(String)}, may be <code>null</code>
     */
    private static void setExec(Object exec, String id, String command, String args, String pipeFrom) {
        setExec(exec, id, command, args, pipeFrom, null, null);
    }
    /**
     * Method filling the {@link PipedExecBuilder.Script} class without working dirset -
     * {@link PipedExecBuilder.Script#setWorkingDir(String)} is not called.
     *
     * @param exec the instance to fill, <b>must not</b> be <code>null</code>.
     * @param id {@link PipedExecBuilder.Script#setID(String)}, may be <code>null</code>
     * @param command {@link PipedExecBuilder.Script#setCommand(String)}, may be <code>null</code>
     *        Note that the command must be supported by {@link SysUtilMock} class!
     * @param args {@link PipedExecBuilder.Script#setArgs(String)}, may be <code>null</code>
     * @param pipeFrom {@link PipedExecBuilder.Script#setPipeFrom(String)}, may be <code>null</code>
     * @param waitFor {@link PipedExecBuilder.Script#setWaitFor(String)}, may be <code>null</code>
     */
    private static void setExec(Object exec, String id, String command, String args, String pipeFrom,
            String waitFor) {
        setExec(exec, id, command, args, pipeFrom, waitFor, null);
    }
    /**
     * Method filling all the attributes of the {@link PipedExecBuilder.Script} class.
     *
     * @param exec the instance to fill, <b>must not</b> be <code>null</code>.
     * @param id {@link PipedExecBuilder.Script#setID(String)}, may be <code>null</code>
     * @param command {@link PipedExecBuilder.Script#setCommand(String)}, may be <code>null</code>
     *        Note that the command must be supported by {@link SysUtilMock} class!
     * @param args {@link PipedExecBuilder.Script#setArgs(String)}, may be <code>null</code>
     * @param pipeFrom {@link PipedExecBuilder.Script#setPipeFrom(String)}, may be <code>null</code>
     * @param waitFor {@link PipedExecBuilder.Script#setWaitFor(String)}, may be <code>null</code>
     * @param workingDir {@link PipedExecBuilder.Script#setWorkingDir(String)}, may be <code>null</code>
     */
    private static void setExec(Object exec, String id, String command, String args,
            String pipeFrom, String waitFor, String workingDir) {

        if (id != null) {
            ((PipedExecBuilder.Script) exec).setID(id);
        }
        if (command != null) {
            /* Find the command among the public attributes of SysUtilMock class */
            try {
                Field f = SysUtilMock.class.getDeclaredField(command);
                String[] c = (String[]) f.get(null);

                /* add -V for verbose output in debug mode */
                if (debugMode) {
                    args = " -V " + (args != null ? args : "");
                }
                /* Update the command and the arguments */
                command = c[0];
                args = c[1] + "-P 'ID " + id + "' " + (args != null ? args : "");

            } catch (NoSuchFieldException e) {
                /* Command is not supported, fail the test */
                fail(SysUtilMock.class.getName() + " does not suport command '" + command
                        + "': " + e.getMessage());
            } catch (Exception e) {
                fail(e.getMessage());
            }
            /* Set the command */
            ((PipedExecBuilder.Script) exec).setCommand(command);
        }
        if (args != null) {
            ((PipedExecBuilder.Script) exec).setArgs(args);
        }
        if (workingDir != null) {
            ((PipedExecBuilder.Script) exec).setWorkingDir(workingDir);
        }
        if (waitFor != null) {
            ((PipedExecBuilder.Script) exec).setWaitFor(waitFor);
        }
        if (pipeFrom != null) {
            ((PipedExecBuilder.Script) exec).setPipeFrom(pipeFrom);
        }
    }

    /**
     * Method reading two files, comparing one against the another.
     *
     * @param  refrFile reference file.
     * @param  testFile tested file.
     * @throws IOException if files cannot be handled.
     */
    private static void assertFiles(File refrFile, File testFile)
        throws IOException {

        /* Both files must exist */
        assertTrue("Reference file " + refrFile, refrFile.exists());
        assertTrue("Tested file " + testFile, refrFile.exists());
        /* Test streams */
        assertStreams(new FileInputStream(refrFile), new FileInputStream(testFile));
  }

    /**
     * Method reading two streams, comparing one against the another. As text files are expected
     * under the streams, {@link BufferedReader} class is used to read from the streams and the
     * lines are compared, actually.
     *
     * @param  refrStream reference stream.
     * @param  testStream tested stream.
     * @throws IOException if streams cannot be handled.
     */
    private static void assertStreams(InputStream refrStream, InputStream testStream)
        throws IOException {

        /* Create readers */
        final BufferedReader refrReader = new BufferedReader(new InputStreamReader(refrStream));
        final BufferedReader testReader = new BufferedReader(new InputStreamReader(testStream));
        int numLinesRead  = 0;
        /* Read and compare line by line */
        while (true) {
            String refrLine = refrReader.readLine();
            String testLine = testReader.readLine();

            /* Leave if one of them is Null */
            if (refrLine == null && testLine == null) {
                break;
            }
            /* Compare lines */
            assertEquals("Line " + ++numLinesRead, refrLine, testLine);
        }
        /* Close them */
        refrReader.close();
        testReader.close();
  }

    /**
     * Method comparing actual string with the required string represented as regular
     * expression. It is almost equal to {@link #assertEquals(String, String, String)} with the
     * difference that required string can be set as regular expression.
     *
     * @param message what is printed in case of failure
     * @param expected the expected format of the message (as regular expression)
     * @param actual actual message to check.
     */
    private static void assertRegex(String message, String expected, String actual)
    {
        if (Pattern.matches(expected, actual)) {
            return;
        }
        /* Not passed - how to print expected/actual message? */
        assertEquals(message, "regex[" + expected + "]", actual);
    }

    /**
     * If {@link #debugMode} in on, prints the build log XML element to STDOUT.
     */
    private static void printXML(final Element buildLog)
    {
        if (! debugMode) {
            return;
        }

        try {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            System.out.println(out.outputString(new Document(buildLog)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
