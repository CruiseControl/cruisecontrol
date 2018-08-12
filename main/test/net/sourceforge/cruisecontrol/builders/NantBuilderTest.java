/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit Copyright (c) 2001,
 * ThoughtWorks, Inc. 200 E. Randolph, 25th Floor Chicago, IL 60601 USA All
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.MockCommandline;
import net.sourceforge.cruisecontrol.util.MockProcess;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom2.Attribute;
import org.jdom2.CDATA;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

public class NantBuilderTest extends TestCase {

    private final FilesToDelete filesToDelete = new FilesToDelete();
    private NantBuilder builder;

    static class InputBasedMockCommandLineBuilder {
        Commandline buildCommandline(final InputStream inputStream) {
            final MockCommandline mockCommandline = getMockCommandline();
            mockCommandline.setAssertCorrectCommandline(false);
            mockCommandline.setProcessErrorStream(new PipedInputStream());
            mockCommandline.setProcessInputStream(inputStream);
            mockCommandline.setProcessOutputStream(new PipedOutputStream());
            return mockCommandline;
        }

        MockCommandline getMockCommandline() {
            return new MockCommandline();
        }
    }

    // process that times out...
    static class TimeoutProcess extends MockProcess {
        private final long timeoutMillis;
        TimeoutProcess(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
        public synchronized void destroy() {
            notifyAll();
        }
        public int waitFor() throws InterruptedException {
            synchronized (this) {
                try {
                    this.wait(timeoutMillis);
                } catch (InterruptedException e) {
                }
            }
            return super.waitFor();
        }
    }


    protected void setUp() throws Exception {
        builder = new NantBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");

        // Must be a cleaner way to do this...
//        builder.setNantWorkingDir(new File(
//                new URI(ClassLoader.getSystemResource("test.build").toString())).getParent());

        final File tempDir = new File(System.getProperty("java.io.tmpdir"));
        final File rootTempDir = new File(tempDir, "testRoot");
        rootTempDir.mkdir();
        filesToDelete.add(rootTempDir);
    }

    public void tearDown() {
        filesToDelete.delete();
        builder = null;
    }

    public void testValidate() {
        builder = new NantBuilder();

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("nantbuilder is missing required attributes");
        }

        builder.setTime("0100");
        builder.setBuildFile("buildfile");
        builder.setTarget("target");

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("validate should not throw exceptions when options are set.");
        }

        builder.setSaveLogDir("I/hope/this/dir/does/not/exist/");
        try {
            builder.validate();
            fail("validate should throw exceptions when saveLogDir doesn't exist");
        } catch (CruiseControlException e) {
        }

        builder.setSaveLogDir(null);
        builder.setMultiple(2);

        try {
            builder.validate();
            fail("validate should throw exceptions when multiple and time are both set.");
        } catch (CruiseControlException e) {
        }
    }

    public void testTranslateNantErrorElementsWithBuildResultsErrorAttribute()
        throws CruiseControlException, DataConversionException {
        Element buildLogElement = new Element("buildresults");
        Attribute errorAttribute = new Attribute("error", "true");
        buildLogElement.setAttribute(errorAttribute);
        buildLogElement = builder.translateNantErrorElements(buildLogElement);
        assertEquals("build", buildLogElement.getName());
        assertTrue(buildLogElement.getAttribute("error").getBooleanValue());

    }

    public void testTranslateNantErrorElementsWithFailureElements()
        throws CruiseControlException {
        Element buildLogElement = new Element("buildresults");
        Element failureElement = new Element("failure");
        buildLogElement.addContent(failureElement);

        try {
            buildLogElement = builder.translateNantErrorElements(buildLogElement);
            fail("Expected a CruiseControlException for invalid nant log output format");
        } catch (CruiseControlException e) { /** expected **/ }

        Element buildErrorElement = new Element("builderror");
        failureElement.addContent(buildErrorElement);

        try {
            buildLogElement = builder.translateNantErrorElements(buildLogElement);
            fail("Expected a CruiseControlException for invalid nant log output format");
        } catch (CruiseControlException e) { /** expected **/ }

        Element messageElement = new Element("message");
        buildErrorElement.addContent(messageElement);

        try {
            buildLogElement = builder.translateNantErrorElements(buildLogElement);
            fail("Expected a CruiseControlException for invalid nant log output format");
        } catch (CruiseControlException e) { /** expected **/ }

        messageElement.setContent(new CDATA("test failure"));

        buildLogElement = builder.translateNantErrorElements(buildLogElement);
        assertEquals("build", buildLogElement.getName());
        Attribute errorAttribute = buildLogElement.getAttribute("error");
        assertNotNull(errorAttribute);
        assertEquals(Attribute.UNDECLARED_TYPE, errorAttribute.getAttributeType());
        assertEquals("test failure", errorAttribute.getValue());
    }

    public void testBuild() throws Exception {

        final String logName = "nantbuilder-build1.txt";
        final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());

        final NantBuilder mybuilder = new NantBuilder() {
            protected NantScript getNantScript() {
                return new NantScript() {
                    public Commandline getCommandLine() {
                        return new InputBasedMockCommandLineBuilder().buildCommandline(emptyInputStream);
                    }
                };
            }
            protected Element getNantLogAsElement(File file) throws CruiseControlException {
                assertEquals("notLog.xml", file.getPath());
                final URL resource = getClass().getResource(logName);
                assertNotNull("missing test case resource: " + logName, resource);
                String path = resource.getPath();
                try {
                    path = URLDecoder.decode(path, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                File simulatedFile;
                try {
                    //done this way to handle spaces in the full pathname
                    simulatedFile = new File(new URI(resource.toString()));
                } catch (URISyntaxException e) {
                    throw new CruiseControlException(e);
                }
                return super.getNantLogAsElement(simulatedFile);
            }
        };
        mybuilder.setTarget("target");
        mybuilder.setBuildFile("buildfile");

        mybuilder.setBuildFile("test.build");
        mybuilder.setTempFile("notLog.xml");
        mybuilder.setTarget("init");
        final Map<String, String> buildProperties = new HashMap<String, String>();
        Element buildElement = mybuilder.build(buildProperties, null);
        int initCount = getInitCount(buildElement);
        assertEquals(1, initCount);

        // TODO: Don't know if this is a valid test with NAnt's file format. Need to verify or convert to Ant format.
//        builder.setTarget("init init");
//        buildElement = builder.build(buildProperties);
//        initCount = getInitCount(buildElement);
//        assertEquals(2, initCount);
    }

    public void testGetCommandLineArgs_DebugAndQuiet() {
        builder.setUseDebug(true);
        builder.setUseQuiet(true);
        try {
            builder.validate();
            fail("validate() should throw CruiseControlException when both useDebug and useQuiet are true");
        } catch (CruiseControlException expected) {
        }
    }

    public int getInitCount(Element buildElement) {
        int initFoundCount = 0;
        Iterator targetIterator = buildElement.getChildren("target").iterator();
        String name;
        while (targetIterator.hasNext()) {
            name = ((Element) targetIterator.next()).getAttributeValue("name");
            if (name.equals("init")) {
                initFoundCount++;
            }
        }
        return initFoundCount;
    }

    public void testBuildTimeout() throws Exception {

        final int buildSleepSecs = 2;

        final MockProcess timeoutProcess = new TimeoutProcess(buildSleepSecs * 1000);
        final MockCommandline timeoutCommandline = new MockCommandline() {
            public MockProcess getMockProcess()  {
                return timeoutProcess;
            }
        };
        final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());
        timeoutCommandline.setAssertCorrectCommandline(false);
        timeoutCommandline.setProcessErrorStream(emptyInputStream);
        timeoutCommandline.setProcessInputStream(emptyInputStream);
        timeoutCommandline.setProcessOutputStream(System.out);

        final NantBuilder mybuilder = new NantBuilder() {
            protected NantScript getNantScript() {
                return new NantScript() {
                    public Commandline getCommandLine() {
                        return timeoutCommandline;
                    }
                };
            }
            protected Element getNantLogAsElement(File file) throws CruiseControlException {
                fail("We should time out... we have nothing to read anyway");
                return super.getNantLogAsElement(file); // please compiler
            }
        };

        mybuilder.setBuildFile("test.build");
        mybuilder.setTarget("timeout-test-target");
        final int testTimeoutSecs = buildSleepSecs / 2;
        mybuilder.setTimeout(testTimeoutSecs);
        mybuilder.setUseDebug(true);
        mybuilder.setUseLogger(true);

        final Map<String, String> buildProperties = new HashMap<String, String>();
        long startTime = System.currentTimeMillis();
        Element buildElement = mybuilder.build(buildProperties, null);
        final long elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
        assertTrue("Too much time has elapsed (" + elapsedMillis + " millis) for NantBuilder timeout of "
                + testTimeoutSecs + " secs.",
                elapsedMillis < ((testTimeoutSecs + 5) * 1000L));

        // test we don't fail when there is no NAnt log file
        mybuilder.setTimeout(1);
        mybuilder.setUseDebug(false);
        mybuilder.setUseLogger(false);
        mybuilder.setTempFile("shouldNot.xml");
        buildElement = mybuilder.build(buildProperties, null);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
    }

    public void testSaveNantLog() throws IOException {
        final String originalDirName = "target";
        filesToDelete.add(new File(TestUtil.getTargetDir(), originalDirName));
        final String logName = "log.xml";
        final String saveDirName = "target/reports/nant";

        builder.setSaveLogDir(saveDirName);
        builder.setTempFile(logName);

        final File originalDir = new File(originalDirName);
        final File originalLog = new File(originalDir, logName);
        Util.doMkDirs(originalDir);
        originalLog.createNewFile();

        final File saveDir = new File(saveDirName);
        final File savedLog = new File(saveDir, logName);
        Util.doMkDirs(saveDir);
        savedLog.delete();

        builder.saveNantLog(originalLog);
        assertTrue(savedLog.exists());

        savedLog.delete();

        builder.setSaveLogDir("");
        builder.saveNantLog(originalLog);
        assertFalse(savedLog.exists());

        builder.setSaveLogDir(null);
        builder.saveNantLog(originalLog);
        assertFalse(savedLog.exists());
    }

    public void testGetNantLogAsElement() throws CruiseControlException {
        Element buildLogElement = new Element("build");
        File logFile = new File("_tempNantLog.xml");
        filesToDelete.add(logFile);
        IO.write(logFile,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<?xml-stylesheet "
                + "type=\"text/xsl\" href=\"log.xsl\"?>\n<build></build>");

        assertEquals(buildLogElement.toString(), builder.getNantLogAsElement(logFile).toString());
    }

    public void testGetNantLogAsElement_NoLogFile() {
        File doesNotExist = new File("blah blah blah does not exist");
        try {
            builder.getNantLogAsElement(doesNotExist);
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("NAnt logfile " + doesNotExist.getAbsolutePath() + " does not exist.", expected.getMessage());
        }
    }

}
