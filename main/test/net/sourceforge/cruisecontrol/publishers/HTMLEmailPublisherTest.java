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

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import java.io.IOException;

public class HTMLEmailPublisherTest extends TestCase {

    private HTMLEmailPublisher publisher;
    private File tmpFile;

    public void setUp() {
        publisher = new HTMLEmailPublisher();
    }

    private class BrokenTestPublisher extends HTMLEmailPublisher {
        private String[] newXslFileNames = null;

        BrokenTestPublisher() {
            setXSLFileNames(newXslFileNames);
        }
    }

    /**
     * test is set up this way because I expect setXSLFileNames to be used
     * from derived clases, so the test mirrors the expected use.
     */
    public void testSetXSLFileNames() {
        try {
            new BrokenTestPublisher();
            fail("setXSLFileNames should fail when called with null");
        } catch (IllegalArgumentException e) {
            // should fail
        }
    }

    public void testSetLogDir() {
        try {
            publisher.setLogDir(null);
            fail("setLogDir should fail when called with null");
        } catch (IllegalArgumentException e) {
            // should fail
        }
    }

    private void checkFileNames(String dir) throws CruiseControlException {
        String[] fileNames = publisher.getXslFileNames();
        if (fileNames == null) {
            throw new CruiseControlException("HTMLEmailPublisher.getXslFileNames() can't return null");
        }

        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            File file = new File(dir, fileName);
            if (!file.exists()) {
                throw new CruiseControlException(
                    fileName + " does not exist: " + file.getAbsolutePath());
            }
            if (!file.isFile()) {
                throw new CruiseControlException(
                    fileName + " is not a file: " + file.getAbsolutePath());
            }
        }
    }

    public void testSetXSLFileList() throws IOException {
        try {
            publisher.setXSLFileList(null);
            fail("setXSLFileList should fail when called with null");
        } catch (IllegalArgumentException ex) {
            // should fail
        }
        try {
            publisher.setXSLFileList("");
            fail("should fail if specified xslFileList file is empty");
        } catch (IllegalArgumentException ex) {
            // should fail
        }
        
        tmpFile =  File.createTempFile("HTMLEmailPublisherTest", null);
        tmpFile.deleteOnExit();
        publisher.setXSLDir(tmpFile.getParent());
        String[] origFileNames = publisher.getXslFileNames();
        publisher.setXSLFileList(tmpFile.getName());
        String[] newFileNames = publisher.getXslFileNames();
        try {
            checkFileNames(tmpFile.getParent());
        } catch (CruiseControlException ex) {
            fail("setXSLFileList with single filename failed to validate:\n" + ex);
        }

        assertEquals(1, newFileNames.length);

        // should work, regardless of spaces & comma between filenames
        publisher.setXSLFileList("  ,, " 
                                 + tmpFile.getName() 
                                 + "   ,,,"
                                 + tmpFile.getName());
        newFileNames = publisher.getXslFileNames();
        try {
            checkFileNames(tmpFile.getParent());
        } catch (CruiseControlException ex) {
            fail("setXSLFileList with two filenames failed to validate");
        }
        assertEquals(2, newFileNames.length);

        // append should work
        publisher.setXSLFileList("+" + tmpFile.getName());
        newFileNames = publisher.getXslFileNames();
        try {
            checkFileNames(tmpFile.getParent());
        } catch (CruiseControlException ex) {
            fail("setXSLFileList, append mode failed to validate");
        }
        
        assertEquals(3, newFileNames.length);

        // should work, if leading spaces
        publisher.setXSLFileList("     +" + tmpFile.getName());
        newFileNames = publisher.getXslFileNames();
        try {
            checkFileNames(tmpFile.getParent());
        } catch (CruiseControlException ex) {
            fail("setXSLFileList, append with leading spaces failed to validate");
        }
        assertEquals(4, newFileNames.length);

        // should fail if some files exist, but some don't
        publisher.setXSLFileList(tmpFile.getName()
                                 + " "
                                 + "this-file-does-not-exist");
        try {
            checkFileNames(tmpFile.getParent());
            fail ("should fail if some xslFileList exist, but some files don't");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFileList("+"
                                 + tmpFile.getName()
                                 + " "
                                 + "this-file-does-not-exist");
        try {
            checkFileNames(tmpFile.getParent());
            fail ("should fail to append if some xslFileList exist, but some files don't");
        } catch (CruiseControlException ex) {
            // should fail
        }
        
        publisher.setXSLFileList("+ " + tmpFile.getName());
        try {
            checkFileNames(tmpFile.getParent());
            fail ("should fail if space between leading '+' and first file in xslFileList");
        } catch (CruiseControlException ex) {
            // should fail
        }
    }

    public void testCreateLinkLine() {
        String serverURL = "http://myserver/context/servlet";
        publisher.setBuildResultsURL(serverURL);
        String path = "logs" + File.separator;
        String date = "20020607115519";
        String label = "mylabel.100";

        String successFilePrefix = "log" + date + "L" + label;
        String successURL = serverURL + "?log=" + successFilePrefix;
        String successLink = "View results here -> <a href=\"" + successURL + "\">" + successURL + "</a>";
        String successFile = successFilePrefix + ".xml";
        String successLogFileName = path + successFile;
        assertEquals(successLink, publisher.createLinkLine(successLogFileName));

        publisher.setBuildResultsURL(null);
        assertEquals("", publisher.createLinkLine(successLogFileName));

        publisher.setBuildResultsURL(serverURL);
        String failFilePrefix = "log" + date;
        String failURL = serverURL + "?log=" + failFilePrefix;
        String failLink = "View results here -> <a href=\"" + failURL + "\">" + failURL + "</a>";
        String failFile = failFilePrefix + ".xml";
        String failLogFileName = path + failFile;
        assertEquals(failLink, publisher.createLinkLine(failLogFileName));
    }

    public void testQuestionMarkInBuildResultsURL() {
        String serverURL = "http://myserver/context/servlet?key=value";
        publisher.setBuildResultsURL(serverURL);
        String path = "logs" + File.separator;
        String date = "20020607115519";
        String label = "mylabel.100";

        String successFilePrefix = "log" + date + "L" + label;
        String successURL = serverURL + "&log=" + successFilePrefix;
        String successLink = "View results here -> <a href=\"" + successURL + "\">" + successURL + "</a>";
        String successFile = successFilePrefix + ".xml";
        String successLogFileName = path + successFile;

        assertEquals(successLink, publisher.createLinkLine(successLogFileName));
    }

    public void testValidate() {
        setEmailPublisherVariables(publisher);

        try {
            publisher.validate();
            fail("should fail if log dir is not set");
        } catch (CruiseControlException ex) {
            // should fail
        }
        publisher.setLogDir(".");

        try {
            publisher.validate();
            fail("should fail if xslDir is not set");
        } catch (CruiseControlException ex) {
            // should fail
        }
        publisher.setXSLDir(".");

        String[] origFileNames = publisher.getXslFileNames();
        try {
            publisher.validate();
            fail("should fail if xslFileNames is null");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFileNames(origFileNames);
        publisher.setXSLFile("this file doesn't exist");
        try {
            publisher.validate();
            fail("should fail if the specified xslFile doesn't exist");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFileNames(origFileNames);
        publisher.setXSLFileList("this-file-doesn't-exist");
        try {
            publisher.validate();
            fail("should fail if specified xslFileList file doesn't exist");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFileNames(origFileNames);
        publisher.setXSLFileList("+this-file-doesn't-exist");
        try {
            publisher.validate();
            fail("should fail if sepcified xslFileList file to append doesn't exist");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFileNames(origFileNames);
        publisher.setXSLFileList(".");
        try {
            publisher.validate();
            fail("should fail if xslFileList file is a directory");
        } catch (CruiseControlException ex) {
            // should fail
        }
    }
    
    public void testGetContentType() {
        String defaultType = "text/html";
        assertEquals(defaultType, publisher.getContentType());
        publisher.setCharset("ISO-8859-1");
        String withCharset = "text/html; charset=\"ISO-8859-1\"";
        assertEquals(withCharset, publisher.getContentType());
    }

    private void setEmailPublisherVariables(HTMLEmailPublisher htmlemailpublisher) {
        htmlemailpublisher.setBuildResultsURL("url");
        htmlemailpublisher.setMailHost("host");
        htmlemailpublisher.setReturnAddress("address");
    }

}
