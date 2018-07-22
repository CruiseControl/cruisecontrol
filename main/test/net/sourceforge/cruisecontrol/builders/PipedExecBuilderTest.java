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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.tools.ant.filters.StringInputStream;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.StdoutBufferTest;


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
        if (debugMode && numlines > 5) {
            numlines = 5;
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

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        addScript(builder, null, new ExecScriptMock.Cat(), null, "01"); // corrupted
        addScript(builder, "03", new ExecScriptMock.Cat(), null, "01");

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when required ID attribute is not set.");

        } catch (CruiseControlException e) {
            assertEquals("exception message when the required ID attribute is not set",
                    "'ID' is required for ExecScriptMock$Cat", e.getMessage());
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

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", null);
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", null); // corrupted

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

        //        builder, ID,   script,                pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(),  null, null);
        addScript(builder, "02", new ExecScriptMock.Cat(),  "xx", null); // corrupted
        addScript(builder, "03", new ExecScriptMock.Cat(),  "02", null);

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

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", null);
        addScript(builder, "03", new ExecScriptMock.Cat(), "02", null);
        /**/
        addScript(builder, "10", new ExecScriptMock.Cat(), null, "xx"); // corrupted
        addScript(builder, "11", new ExecScriptMock.Cat(), "10", null);

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

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", null);
        addScript(builder, "03", new ExecScriptMock.Cat(), "02", null);
        addScript(builder, "04", new ExecScriptMock.Cat(), "03", null);
        addScript(builder, "05", new ExecScriptMock.Cat(), "04", null);
        /**/
        addScript(builder, "10", new ExecScriptMock.Cat(), "20", null); // piped from 20
        addScript(builder, "11", new ExecScriptMock.Cat(), "10", null);
        addScript(builder, "12", new ExecScriptMock.Cat(), "11", null);
        addScript(builder, "13", new ExecScriptMock.Cat(), "12", null);
        /**/
        addScript(builder, "20", new ExecScriptMock.Cat(), "12", null); // loop start

        /* Must not be validated */
        try {
            builder.validate();
            fail("PipedExecBuilder should throw an exception when the pipe loop is detected.");

        } catch (CruiseControlException e) {
            assertRegex("exception message when pipe loop is detected",
                    "Loop detected, ID \\d\\d is within loop", e.getMessage());
        }

        /* Disable ID 11 to break the loop */
        setDisble(builder, "11");
        /* And validate again. Now it must pass */
        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail(e.getMessage());
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

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        // wait for 20 which depends on output of 02
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", "20");
        addScript(builder, "03", new ExecScriptMock.Cat(), "02", null);
        addScript(builder, "04", new ExecScriptMock.Cat(), "03", null);
        addScript(builder, "05", new ExecScriptMock.Cat(), "04", null);
        /**/
        addScript(builder, "10", new ExecScriptMock.Cat(), "02", null);
        addScript(builder, "11", new ExecScriptMock.Cat(), "10", null);
        addScript(builder, "12", new ExecScriptMock.Cat(), "11", null);
        addScript(builder, "13", new ExecScriptMock.Cat(), "12", null);
        /**/
        addScript(builder, "20", new ExecScriptMock.Cat(), "12", null);

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
     * Checks the validation when an already existing script is re-piped
     * <pre>
     *  orig:   01 --> 02 --> 03 -->
     *  repipe: 01 --> 10 --> 11 --> 02 --> 03 -->
     * </pre>
     * @throws CruiseControlException
     */
    public void testValidate_repipe() throws CruiseControlException {
        PipedExecBuilder builder  = new PipedExecBuilder();

        //        builder, ID,   script,               pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(), null, null);
        addScript(builder, "02", new ExecScriptMock.Cat(), "01", null);
        addScript(builder, "03", new ExecScriptMock.Cat(), "02", null);
        /* add for new pipe */
        addScript(builder, "10", new ExecScriptMock.Cat(), "01", null);
        addScript(builder, "11", new ExecScriptMock.Cat(), "10", null);

        /* repipe now - define only ID (again) and pipe */
        setRepipe(builder, "02",                           "11");

        /* Must be validated correctly */
        builder.validate();
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
        File inpFile = files.add(this);
        File tmpFile = files.add(this);

        /* Create the content */
        createFiles(inpFile, tmpFile, 200);

        /* First cat - simulated by stream reader */
        InputStream cat1 = new BufferedInputStream(new FileInputStream(inpFile));

        /* Second cat - real command without arguments */
        PipedScript cat2 = new ExecScriptMock.Cat();
        builder.add(cat2);
        cat2.setID("2");
        cat2.initialize();
        cat2.setInputProvider(cat1);
        cat2.setBuildProperties(new HashMap<String, String>());
        cat2.setBuildLogParent(new Element("build"));
        cat2.setBinaryOutput(false);
        cat2.setGZipStdout(false);
        /* Validate and run (not as thread here) */
        cat2.validate();
        cat2.run();

        /* Test the output - it must be the same as the input */
        assertStreams(new FileInputStream(inpFile), cat2.getOutputReader());

        /* Third cat - real command without arguments */
        PipedScript cat3 = new ExecScriptMock.Cat();
        builder.add(cat3);
        cat3.setID("3");
        cat3.initialize();
        cat3.setInputProvider(cat2.getOutputReader());
        cat3.setBuildProperties(new HashMap<String, String>());
        cat3.setBuildLogParent(new Element("build"));
        cat3.setBinaryOutput(false);
        cat3.setGZipStdout(false);
        /* Validate and run (not as thread here) */
        cat3.validate();
        cat3.run();

        /* Test the output - it must be the same as the input */
        assertStreams(new FileInputStream(inpFile), cat3.getOutputReader());
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

        PipedExecBuilder builder   = new PipedExecBuilder();
        Element buildLog;

        /* Input and result files */
        File inp1File = files.add(this);
        File inp2File = files.add(this);
        File res1File = files.add(this);
        File res2File = files.add(this);
        /* Prepare content */
        createFiles(inp1File, res1File, 50);
        createFiles(inp2File, res2File, 110);

        /* Temporary and output files */
        File tmp1File = files.add(this);
        File tmp2File = files.add(this);
        File out1File = files.add(this);
        File out2File = files.add(this);
        File out3File = files.add(this);
        File out4File = files.add(this);


        /* Fill it by commands to run*/
        builder.setShowProgress(false);
        builder.setTimeout(180);
        //        builder, ID,   script,                                                     pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat("<"+tmp2File.getAbsolutePath()),         null, "37");
        addScript(builder, "02", new ExecScriptMock.Sort(),                                      "01", null);
        addScript(builder, "03", new ExecScriptMock.Uniq(),                                      "02", null);
        addScript(builder, "04", new ExecScriptMock.Cat(">"+out4File.getAbsolutePath()),         "03", null);
        /**/
        addScript(builder, "11", new ExecScriptMock.Cat("<"+inp2File.getAbsolutePath()),         null, null);
        addScript(builder, "12", new ExecScriptMock.Cat("<"+tmp1File.getAbsolutePath() + " <-"), "11", "35");
        addScript(builder, "13", new ExecScriptMock.Shuf(),                                      "12", null);
        addScript(builder, "14", new ExecScriptMock.Grep("^\\s*([0-9]+\\s+){3}"),                "13", null);
        addScript(builder, "15", new ExecScriptMock.Del(),                                       "14", null);
        addScript(builder, "16", new ExecScriptMock.Sort(),                                      "15", null);
        addScript(builder, "17", new ExecScriptMock.Uniq(),                                      "16", null);
        addScript(builder, "18", new ExecScriptMock.Grep("^\\s*([0-9]+\\s+){3}", false),         "13", null);
        addScript(builder, "19", new ExecScriptMock.Sort(true),                                  "18", null);
        addScript(builder, "20", new ExecScriptMock.Cat(">"+out2File.getAbsolutePath()),         "19", null);
        addScript(builder, "21", new ExecScriptMock.Cat(">"+out3File.getAbsolutePath()),         "17", null);
        /**/
        addScript(builder, "31", new ExecScriptMock.Cat("<"+inp1File.getAbsolutePath()),         null, null);
        addScript(builder, "32", new ExecScriptMock.Cat(),                                       "31", null);
        addScript(builder, "33", new ExecScriptMock.Add(),                                       "32", null);
        addScript(builder, "34", new ExecScriptMock.Shuf(),                                      "33", null);
        addScript(builder, "35", new ExecScriptMock.Cat(">"+tmp1File.getAbsolutePath()),         "33", null);
        addScript(builder, "36", new ExecScriptMock.Del(),                                       "34", null);
        addScript(builder, "37", new ExecScriptMock.Cat(">"+tmp2File.getAbsolutePath()),         "36", null);
        addScript(builder, "38", new ExecScriptMock.Sort(),                                      "36", null);
        addScript(builder, "39", new ExecScriptMock.Uniq(),                                      "38", null);
        addScript(builder, "40", new ExecScriptMock.Cat(">"+out1File.getAbsolutePath()),         "39", null);

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

        PipedExecBuilder builder = new PipedExecBuilder();
        Element buildLog;
        long startTime = System.currentTimeMillis();

        /* Set 2 minutes long timeout. */
        builder.setTimeout(120);
        builder.setShowProgress(false);

        /* Read from stdin, but do not have it piped. The command must end immediately
         * without error, as the stdin MUST BE closed when determined that nothing can be
         * read from */
        //        builder, ID,   script,                    pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat(),      null, null);
        addScript(builder, "02", new ExecScriptMock.Sort(true), "01", null);

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
     * Tests the repining the validation when an already existing script is re-piped
     * <pre>
     *                           +-> 06(file)
     *  orig:   01 --> 02 --> 03 +-> 04 --> 05(file)
     *
     *  repipe: 01 --> 10 --> 11 --> 12 --> 13 +-> 04 --> 05(file)
     *                                         +-> SO(file)
     * </pre>
     * @throws IOException
     * @throws CruiseControlException
     */
    public void testBuild_repipe() throws IOException, CruiseControlException {
        PipedExecBuilder builder  = new PipedExecBuilder();

        /* Input file (in UTF8 encoding), and output files */
        File inpFile = files.add(this);
        File ou1File = files.add(this);
        File ou2File = files.add(this);
        File ou3File = files.add(this);
        File tmpFile = files.add(this);

        /* Out 3 must not exist */
        ou3File.delete();
        assertFalse(ou3File.exists());

        /* Create the content */
        createFiles(inpFile, tmpFile, 200);

        /* Set 2 minutes long timeout. */
        builder.setTimeout(120);
        builder.setShowProgress(false);

        //        builder, ID,   script,                                            pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat("<"+inpFile.getAbsolutePath()), null, null);
        addScript(builder, "02", new ExecScriptMock.Add(),                              "01", null);
        addScript(builder, "03", new ExecScriptMock.Shuf(),                             "02", null);
        addScript(builder, "04", new ExecScriptMock.Cat(),                              "03", null);
        addScript(builder, "05", new ExecScriptMock.Cat(">"+ou1File.getAbsolutePath()), "04", null);
        addScript(builder, "06", new ExecScriptMock.Cat(">"+ou3File.getAbsolutePath()), "03", null);

        /* Now define the repipe */
        addScript(builder, "10", new ExecScriptMock.Shuf(),                             "01", null);
        addScript(builder, "11", new ExecScriptMock.Cat(),                              "10", null);
        addScript(builder, "12", new ExecScriptMock.Cat(),                              "11", null);
        addScript(builder, "13", new ExecScriptMock.Sort(true),                         "12", null);
        addScript(builder, "S1", new ExecScriptMock.Sort(true),                         "01", null);
        addScript(builder, "S2", new ExecScriptMock.Cat(">"+ou2File.getAbsolutePath()), "S1", null);

        /* Repipe here 04 from 13 (instead of 03)*/
        setRepipe(builder, "04", "13");
        /* And disable the old path */
        setDisble(builder, "02");

        /* Validate it and run it */
        builder.validate();
        builder.build(new HashMap<String, String>(), null);

        /* Check to the sorted variant (the first pile does not sort at all) */
        assertFiles(tmpFile, ou1File);
        assertFiles(tmpFile, ou2File);
        /* out3 file must not exist (since disabled, the path must not be invoked) */
        assertFalse(ou3File.exists());
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
    public void testExec_badCommand() throws IOException, CruiseControlException {
        final PipedExecBuilder builder  = new PipedExecBuilder();
        final PipedExecBuilder.Script badScript = builder.new Script();

        /* Fill script with valid arguments but with non-existing command */
        badScript.setTimeout(300000);
        badScript.setCommand("BAD");  /* Such binary should not exist on most of the platforms */

        /* Output and "somewhere-in-the middle" temporary file, input and corresponding result
         * files */
        File out1File = files.add(this);
        File inp1File = files.add(this);
        File res1File = files.add(this);
        /* Prepare content */
        createFiles(inp1File, res1File, 50);

        //        builder, ID,   script,                                             pipeFrom, waitFor
        addScript(builder, "01", new ExecScriptMock.Cat("<"+inp1File.getAbsolutePath()), null, null);
        addScript(builder, "02", new ExecScriptMock.Shuf(),                              "01", null);
        addScript(builder, "03", badScript,                                              "02", null); /* corrupted: bad binary name */
        addScript(builder, "04", new ExecScriptMock.Sort(true),                          "02", null);
        addScript(builder, "05", new ExecScriptMock.Cat(">"+out1File.getAbsolutePath()), "04", null);


        /* Validate it and run it */
        builder.validate();
        /* And run */
        final Element buildLog = builder.build(new HashMap<String, String>(), null);
        final Attribute error  = buildLog.getAttribute("error");

        printXML(buildLog);
        /* 'error' attribute must exist in the build log */
        assertNotNull("error attribute was not found in build log!", error);
        assertRegex("unexpected error message is returned", "(exec error.*|return code .*)", error.getValue());
    }


    /**
     * Test environment variables in the build - sets some value PipedExecBuilder and check
     * if it is propagated to the individual builders
     *
     * @throws CruiseControlException
     * @throws IOException
     */
    public void testExec_SetEnvVal() throws IOException, CruiseControlException {
        PipedExecBuilder builder  = new PipedExecBuilder();
        Builder.EnvConf env;
        String envvar = "TESTENV";
        String envval = "dummy_value";

        File envExec = files.add("PipedExecBuilderTest.internalEnvTest", "_outputenv.bat");
        File outFile = files.add(this);

        builder.setTimeout(10);
        builder.setShowProgress(false);


        // Create script printing the env variables to stdout. It must be external script (we use the same
        // as used in ExecBuilderTest), and we must use PipedExecBuilder.createExec() to create the script
        // with env setting linked. This must be solved in a better way ...
        final ExecBuilder envGetter = ExecBuilderTest.createEnvExec(envExec);
        final PipedExecBuilder.Script script = (PipedExecBuilder.Script) builder.createExec();
        script.setCommand(envGetter.getCommand());
        script.setArgs(envGetter.getArgs());
        script.setID("env");
        // Use the "other" scripts to get the env variables to the file
        //        builder, ID,   script,           pipeFrom, waitFor
        addScript(builder, "get", new ExecScriptMock.Grep("^"+envvar+".*"),              "env", null);
        addScript(builder, "out", new ExecScriptMock.Cat(">"+outFile.getAbsolutePath()), "get", null);

        // set env
        env = builder.createEnv();
        env.setName(envvar);
        env.setValue(envval);
        // Validate it and run it
        builder.validate();
        builder.build(new HashMap<String, String>(), null);

        // Test the filtered output.
        assertStreams(new StringInputStream(envvar+"="+envval), new FileInputStream(outFile));
    } // testBuild_NewEnvVar

    /**
     * Checks the timeout function. The pipe looks like:
     * <pre>
     *  01(infinite read) --> 02 --> 03
     * </pre>
     *
     * @throws IOException if the test fails!
     * @throws CruiseControlException if the builder fails!
     */
    public void testExec_timeout() throws CruiseControlException, IOException {

        PipedExecBuilder builder = new PipedExecBuilder();
        Element buildLog;
        Attribute error;

        /* Output files, input and corresponding result files */
        File infScript = files.add("PipedExecBuilderTest.internalTimeoutTest", "_sleep.bat");

        // Create command running 10secs
        // Create script running 10sec. It must be external script (we use the same
        // as used in ExecBuilderTest), and we copy its parameters to the script build by
        // PipedExecBuilder.createExec()
        final ExecBuilder infExec = ExecBuilderTest.createSleepExec(infScript, 10);
        final PipedExecBuilder.Script script = (PipedExecBuilder.Script) builder.createExec();
        script.setCommand(infExec.getCommand());
        script.setArgs(infExec.getArgs());
        script.setID("01");
        // Create other "scripts" to a pipe
        //        builder, ID,   script,                pipeFrom, waitFor
        addScript(builder, "02", new ExecScriptMock.Sort(), "01", null);
        addScript(builder, "03", new ExecScriptMock.Cat(),  "02", null);

        /* Fill it by commands to run. */
        builder.setTimeout(2);
        builder.setShowProgress(false);
        /* Validate it and run it */
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);
        error    = buildLog.getAttribute("error");

        printXML(buildLog);
        /* 'error' attribute must exist in the build log, ant it must hold 'timeout' string */
        assertNotNull("error attribute was not found in build log!", error);
        assertRegex("unexpected error message is returned", "build timeout.*", error.getValue());
    }


    /**
     * Method filling all the attributes of the {@link PipedExecBuilder.Script} class.
     *
     * @param builder the instance to add the new script into
     * @param id {@link PipedExecBuilder.Script#setID(String)}, may be <code>null</code>
     * @param script the instance of {@link ExecScriptMock} to add
     * @param pipeFrom {@link PipedExecBuilder.Script#setPipeFrom(String)}, may be <code>null</code>
     * @param waitFor {@link PipedExecBuilder.Script#setWaitFor(String)}, may be <code>null</code>
     */
    private static void addScript(final PipedExecBuilder builder, final String id, final PipedScript script,
            String pipeFrom, String waitFor) {

        if (id != null) {
            script.setID(id);
        }
        if (waitFor != null) {
            script.setWaitFor(waitFor);
        }
        if (pipeFrom != null) {
            script.setPipeFrom(pipeFrom);
        }

//        // in debug mode, print more details
//        if (debugMode) {
//            System.out.println("Exec: " + exec);
//        }

        builder.add(script);
    }
    /**
     * Method filling the "repipe" script for the given builder.
     *
     * @param builder the instance to set the repipe request into
     * @param ID {@link PipedScript#setID(String)}
     * @param repipe {@link PipedScript#setRepipe(String)}
     */
    private static void setRepipe(final PipedExecBuilder builder, final String ID, final String repipe) {
        assertNotNull(builder);
        assertNotNull(ID);

        final PipedScript script;

        script = builder.createExec();
        script.setID(ID);
        script.setRepipe(repipe);
    }
    /**
     * Method filling the "disable" attributes of the {@link PipedScript} class.
     *
     * @param builder the instance to add the new script into
     * @param id {@link PipedScript#setID(String)}
     */
    private static void setDisble(final PipedExecBuilder builder, final String id) {
        assertNotNull(builder);
        assertNotNull(id);

        final PipedScript script;

        script = builder.createExec();
        script.setID(id);
        script.setDisable(true);
    }

    /**
     * If {@link #debugMode} in on, prints the build log XML element to STDOUT.
     * @param buildLog the element to print.
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
