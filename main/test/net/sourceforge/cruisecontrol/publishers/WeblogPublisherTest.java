/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for
 * {@link net.sourceforge.cruisecontrol.publishers.WeblogPublisher WeblogPublisher}.
 * 
 * @author Lasse Koskela
 */
public class WeblogPublisherTest extends TestCase {

    private static int counter = 1;

    private File xslDir;

    private String[] xslFiles = { "header.xsl", "maven.xsl", "checkstyle.xsl",
            "compile.xsl", "javadoc.xsl", "unittests.xsl", "modifications.xsl",
            "distributables.xsl" };

    private WeblogPublisher publisher;

    protected void setUp() throws Exception {
        publisher = new WeblogPublisher();
        publisher.setBuildResultsURL("http://localhost:8080/cc");
        publisher.setLogDir(createTempDir().getAbsolutePath());
        publisher.setUsername("user123");
        publisher.setPassword("topsecret");
        publisher.setBlogId("myblog");
        publisher.setBlogUrl("http://foobar.com/blog/xmlrpc");

        xslDir = createTempDir();
        for (int i = 0; i < xslFiles.length; i++) {
            createTempFile(xslDir, xslFiles[i]);
        }

        publisher.setXSLDir(xslDir.getAbsolutePath());
        publisher.setCSS(createTempFile(xslDir, "cc.css").getAbsolutePath());
    }

    public void testUsernameIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setUsername(null);
            publisher.validate();
            fail("Validation should fail when the username is not set");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("username") != -1);
        }
    }

    public void testPasswordIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setPassword(null);
            publisher.validate();
            fail("Validation should fail when password is not set");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing attribute",
                    expected.getMessage().indexOf("password") != -1);
        }
    }

    public void testBlogIdIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setBlogId(null);
            publisher.validate();
            fail("Validation should fail when blogid is not set");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing attribute",
                    expected.getMessage().indexOf("blogid") != -1);
        }
    }

    public void testBlogUrlIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setBlogUrl(null);
            publisher.validate();
            fail("Validation should fail when blogurl is not set");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing attribute",
                    expected.getMessage().indexOf("blogurl") != -1);
        }
    }

    public void testBlogUrlMustBeValidUrl() throws Exception {
        publisher.validate();
        String invalidUrl = "htt//thisisnotavalid.url";
        try {
            publisher.setBlogUrl(invalidUrl);
            publisher.validate();
            fail("Validation should fail when blogurl is not a valid URL");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the invalid URL", expected
                    .getMessage().indexOf(invalidUrl) != -1);
        }
    }

    public void testBuildResultsUrlIsNotRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setBuildResultsURL(null);
            publisher.validate();
        } catch (CruiseControlException expected) {
            fail("Validation should fail if buildresultsurl is not set"
                    + "since it's not a required attribute.");
        }
    }

    public void testBuildResultsUrlMustBeValidUrl() throws Exception {
        publisher.validate();
        String invalidUrl = "htt//thisisnotavalid.url";
        try {
            publisher.setBuildResultsURL(invalidUrl);
            publisher.validate();
            fail("Validation should fail when buildresultsurl is not a valid URL");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the invalid URL", expected
                    .getMessage().indexOf(invalidUrl) != -1);
        }
    }

    public void testXslDirIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setXSLDir("doesnotexist");
            publisher.validate();
            fail("Validation should fail when the XSL directory doesn't exist");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing directory's name",
                    expected.getMessage().indexOf("doesnotexist") != -1);
        }
    }

    public void testXslFileMustExist() throws Exception {
        publisher.validate();
        try {
            publisher.setXSLFile("doesnotexist");
            publisher.validate();
            fail("Validation should fail when the XSL file doesn't exist");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing file's name",
                    expected.getMessage().indexOf("doesnotexist") != -1);
        }
    }

    public void testCssFileIsRequired() throws Exception {
        publisher.validate();
        try {
            publisher.setCSS("doesnotexist");
            publisher.validate();
            fail("Validation should fail when the CSS file doesn't exist");
        } catch (CruiseControlException expected) {
            assertTrue("The error should mention the missing file's name",
                    expected.getMessage().indexOf("doesnotexist") != -1);
        }
    }

    public void testXslFilesAreRequired() throws Exception {
        for (int i = 0; i < xslFiles.length; i++) {
            String xslFileName = xslFiles[i];
            publisher.validate();
            try {
                new File(xslDir, xslFileName).delete();
                publisher.validate();
                fail("Validation should fail if " + xslFileName + " is missing");
            } catch (CruiseControlException expected) {
                assertTrue("The error message should include the "
                        + "name of the missing file", expected.getMessage()
                        .indexOf(xslFileName) != -1);
            }
            createTempFile(xslDir, xslFileName);
        }
    }

    public void testBuildResultsUrlIsConstructedCorrectly() throws Exception {
        String serverURL = "http://localhost:8080/cc";
        String expected = serverURL + "?log=LOGFILE";
        publisher.setBuildResultsURL(serverURL);
        assertEquals(expected, publisher.createBuildResultsUrl("LOGFILE.XML"));
    }

    public void testBuildResultsUrlIsConstructedCorrectlyWithQuestionMark() {
        String serverURL = "http://myserver/context/servlet?key=value";
        String expected = serverURL + "&log=LOGFILE";
        publisher.setBuildResultsURL(serverURL);
        assertEquals(expected, publisher.createBuildResultsUrl("LOGFILE.XML"));
    }

    public void testBuildResultsLinkIsConstructedCorrectly() throws Exception {
        String url = publisher.createBuildResultsUrl("TEST.XML");
        String link = "<a href=\"" + url + "\">" + url + "</a>";
        assertTrue(publisher.createLinkLine("TEST.XML").indexOf(link) != -1);
    }

    public void testDefaultLogDirectory() throws Exception {
        String expected = "logs" + File.separator + "myproject";
        assertEquals(expected, publisher.getDefaultLogDir("myproject"));
    }

    public void testCreateSubjectForSuccessfulBuild() throws Exception {
        boolean isSuccessful = true;
        boolean isFix = false;
        String subject = publisher.createSubject("myproject", "mylabel",
                isSuccessful, isFix);
        assertEquals("myproject mylabel - Build Successful", subject);
    }

    public void testCreateSubjectForFailedBuild() throws Exception {
        boolean isSuccessful = false;
        boolean isFix = false;
        String subject = publisher.createSubject("myproject", "mylabel",
                isSuccessful, isFix);
        assertEquals("myproject - Build Failed", subject);
    }

    public void testCreateSubjectForFixedBuild() throws Exception {
        boolean isSuccessful = true;
        boolean isFix = true;
        String subject = publisher.createSubject("myproject", "mylabel",
                isSuccessful, isFix);
        assertEquals("myproject mylabel - Build Fixed", subject);
    }

    public void testCreateSubjectUsesPrefixIfSpecified() throws Exception {
        boolean isSuccessful = true;
        boolean isFix = false;
        publisher.setSubjectPrefix("[CC]");
        String subject = publisher.createSubject("myproject", "mylabel",
                isSuccessful, isFix);
        assertEquals("[CC] myproject mylabel - Build Successful", subject);
    }

    private static class MockXMLLogHelper extends XMLLogHelper {
        private boolean isBuildSuccessful = true; // 'true' by default

        private boolean wasPreviousBuildSuccessful = true; // 'true' by default

        private boolean isBuildNecessary = true; // 'true' by default

        public MockXMLLogHelper() {
            super(null);
        }

        public boolean isBuildSuccessful() {
            return isBuildSuccessful;
        }

        public void setBuildSuccessful(boolean b) {
            this.isBuildSuccessful = b;
        }

        public boolean wasPreviousBuildSuccessful() {
            return wasPreviousBuildSuccessful;
        }

        public void setPreviousBuildSuccessful(boolean b) {
            this.wasPreviousBuildSuccessful = b;
        }

        public boolean isBuildNecessary() {
            return isBuildNecessary;
        }

        public void setBuildNecessary(boolean b) {
            this.isBuildNecessary = b;
        }
    }

    public void testShouldSendWhenBuildIsSuccessful() throws Exception {
        MockXMLLogHelper logHelper = new MockXMLLogHelper();
        logHelper.setPreviousBuildSuccessful(true);
        logHelper.setBuildSuccessful(true);

        publisher.setReportSuccess("always");
        assertTrue(publisher.shouldSend(logHelper));

        publisher.setReportSuccess("fixes");
        assertFalse(publisher.shouldSend(logHelper));

        publisher.setReportSuccess("never");
        assertFalse(publisher.shouldSend(logHelper));
    }

    public void testShouldSendWhenBuildIsFixed() throws Exception {
        MockXMLLogHelper logHelper = new MockXMLLogHelper();
        logHelper.setPreviousBuildSuccessful(false);
        logHelper.setBuildSuccessful(true);

        publisher.setReportSuccess("always");
        assertTrue(publisher.shouldSend(logHelper));

        publisher.setReportSuccess("fixes");
        assertTrue(publisher.shouldSend(logHelper));

        publisher.setReportSuccess("never");
        assertFalse(publisher.shouldSend(logHelper));
    }

    public void testShouldSendWhenBuildFailed() throws Exception {
        MockXMLLogHelper logHelper = new MockXMLLogHelper();
        logHelper.setPreviousBuildSuccessful(true);
        logHelper.setBuildSuccessful(false);

        assertTrue(publisher.shouldSend(logHelper));
    }

    public void testShouldSendWhenBuildFailedManyTimes() throws Exception {
        MockXMLLogHelper logHelper = new MockXMLLogHelper();
        logHelper.setPreviousBuildSuccessful(false);
        logHelper.setBuildSuccessful(false);

        publisher.setSpamWhileBroken(true);
        assertTrue(publisher.shouldSend(logHelper));

        publisher.setSpamWhileBroken(false);
        assertFalse(publisher.shouldSend(logHelper));
    }

    public void testTransformDelegatesCorrectlyWithSingleStylesheetSpecified()
            throws Exception {
        final boolean[] delegatedToTheCorrectMethod = { false };
        publisher = new WeblogPublisher() {
            void transformWithSingleStylesheet(File xml, StringBuffer buf) {
                delegatedToTheCorrectMethod[0] = true;
            }
        };
        publisher.setXSLFile("foo.xsl");
        publisher.transform(new File("foo.xml"));
        assertTrue(delegatedToTheCorrectMethod[0]);
    }

    public void testTransformDelegatesCorrectlyWithoutStylesheetSpecified()
            throws Exception {
        final boolean[] delegatedToTheCorrectMethod = { false };
        publisher = new WeblogPublisher() {
            void transformWithMultipleStylesheets(File xml, StringBuffer buf) {
                delegatedToTheCorrectMethod[0] = true;
            }
        };
        publisher.transform(new File("foo.xml"));
        assertTrue(delegatedToTheCorrectMethod[0]);
    }

    //appendTransform(inFile, xsl, messageBuffer, tFactory);
    public void testAllStylesheetsAreUsedInTransformation() throws Exception {
        final List xslFilesUsed = new ArrayList();
        publisher = new WeblogPublisher() {
            void appendTransform(File xml, File xsl, StringBuffer buf,
                    TransformerFactory tf) {
                xslFilesUsed.add(xsl.getName());
            }
        };
        publisher.setXSLDir("foo");
        publisher.transform(new File("foo.xml"));
        for (int i = 0; i < publisher.getXslFileNames().length; i++) {
            assertTrue("File " + publisher.getXslFileNames()[i]
                    + " was not used for transformation", xslFilesUsed
                    .contains(publisher.getXslFileNames()[i]));
        }
    }

    public void testTheAppendTransformMethodWorksInGeneral() throws Exception {
        File xml = createTempXmlFile();
        File xsl = createTempXslFile();
        StringBuffer buf = new StringBuffer();
        TransformerFactory tfactory = TransformerFactory.newInstance();
        publisher.appendTransform(xml, xsl, buf, tfactory);
        assertEquals("Testing", buf.toString());
    }

    private File createTempXslFile() throws IOException, CruiseControlException {
        File f = createTempFile();
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0'?>").append('\n');
        buf.append("<xsl:stylesheet");
        buf.append(" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"");
        buf.append(" version=\"1.0\">").append('\n');
        buf.append("<xsl:output method=\"text\"/>").append('\n');
        buf.append("<xsl:template match=\"/\">");
        buf.append("<xsl:value-of select=\"just\"/>");
        buf.append("</xsl:template>").append('\n');
        buf.append("</xsl:stylesheet>");

        IO.write(f, buf.toString());
        return f;
    }

    private File createTempXmlFile() throws CruiseControlException, IOException {
        File f = createTempFile();
        IO.write(f, "<?xml version='1.0'?><just>Testing</just>");
        return f;
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("WeblogPublisherTest", ".tmp");
        tempFile.deleteOnExit();
        return tempFile;
    }

    private File createTempFile(File parent, String name) throws CruiseControlException {
        File tempFile = new File(parent, name);
        tempFile.deleteOnExit();
        IO.write(tempFile, "");
        return tempFile;
    }

    private File createTempDir() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDir = new File(tempDir, "tempdir_" + (counter++));
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        return tempDir;
    }

}
