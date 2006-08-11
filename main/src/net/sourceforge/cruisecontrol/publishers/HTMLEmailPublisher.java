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

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.apache.tools.ant.launch.Locator;

/**
 * Used to publish an HTML e-mail that includes the build report
 *
 * @author Jeffrey Fredrick
 * @author Alden Almagro
 * @author <a href="vwiewior@valuecommerce.ne.jp">Victor Wiewiorowski</a>
 */
public class HTMLEmailPublisher extends EmailPublisher {

    private static final Logger LOG = Logger.getLogger(HTMLEmailPublisher.class);

    private String xslFile;
    private String xslDir;
    private String css;
    private String logDir;
    private String messageMimeType = "text/html";
    private String charset;

    // Should reflect the same stylesheets as buildresults.jsp in the JSP
    // reporting application
    private String[] xslFileNames =
        {
            "header.xsl",
            "buildresults.xsl"
        };

    private List xsltParameters = new LinkedList();

    /*
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        super.validate();

        if (logDir != null) {
            verifyDirectory("HTMLEmailPublisher.logDir", logDir);
        } else {
            LOG.debug("Using default logDir \"logs/<projectname>\"");
        }

        if (xslFile == null) {
            if (xslDir == null) {
                // try to obtain the dir relative to the current classpath
                xslDir = getXslDirFromClasspath();
            }
            verifyDirectory("HTMLEmailPublisher.xslDir", xslDir);
            if (css == null) {
                // same for css
                css = getCssFromClasspath();
            }
            verifyFile("HTMLEmailPublisher.css", css);

            String[] fileNames = getXslFileNames();
            
            if (fileNames == null) {
                throw new CruiseControlException("HTMLEmailPublisher.getXslFileNames() can't return null");
            }

            for (int i = 0; i < fileNames.length; i++) {
                String fileName = fileNames[i];
                verifyFile(
                    "HTMLEmailPublisher.xslDir/" + fileName,
                    new File(xslDir, fileName));
            }
        } else {
            verifyFile("HTMLEmailPublisher.xslFile", xslFile);
        }
    }

    /**
     */
    public Property createParameter() {
        Property param = new Property();
        xsltParameters.add(param);
        return param;
    }

    /**
     * @return the absolute path where the cruisecontrol.css file is located,
     * or null if it can't be found.
     */
    private String getCssFromClasspath() {
        File cssFile = new File(getCruiseRootDir(), "reporting/jsp/webcontent/css/cruisecontrol.css");
        if (cssFile.exists()) {
            return cssFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * @return the absolute path where the xsl dir is located,
     * or null if it can't be found.
     */
    private String getXslDirFromClasspath() {
        File xsl = new File(getCruiseRootDir(), "reporting/jsp/webcontent/xsl");
        if (xsl.isDirectory()) {
            return xsl.getAbsolutePath();
        }
        return null;
    }

    /**
     * @return the root directory of the running cruisecontrol installation.
     * Uses Ant's Locator.
     */
    private File getCruiseRootDir() {
        File classDir = Locator.getClassSource(getClass());
        if (classDir != null) {
            try {
                // we're probably in main/dist/cruisecontrol.jar, so three parents up
                File rootDir = classDir.getParentFile().getParentFile().getParentFile();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("rootDir seems to be " + rootDir.getAbsolutePath()
                            + " (classDir = " + classDir.getAbsolutePath() + ")");
                }
                return rootDir;
            } catch (NullPointerException npe) {
                // don't know where we are, then...
                return null;
            }
        }
        return null;
    }

    private void verifyDirectory(String dirName, String dir) throws CruiseControlException {
        ValidationHelper.assertFalse(dir == null, dirName + " not specified in configuration file");
        File dirFile = new File(dir);
        ValidationHelper.assertTrue(dirFile.exists(), dirFile + " does not exist: " + dirFile.getAbsolutePath());
        ValidationHelper.assertTrue(dirFile.isDirectory(),
                dirFile + " is not a directory: " + dirFile.getAbsolutePath());
    }

    private void verifyFile(String fileName, String file) throws CruiseControlException {
        ValidationHelper.assertFalse(file == null, fileName + " not specified in configuration file");
        verifyFile(fileName, new File(file));
    }

    private void verifyFile(String fileName, File file) throws CruiseControlException {
        ValidationHelper.assertTrue(file.exists(), fileName + " does not exist: " + file.getAbsolutePath());
        ValidationHelper.assertTrue(file.isFile(), fileName + " is not a file: " + file.getAbsolutePath());
    }

    /**
     * sets the content as an attachment w/proper mime-type
     */
    protected void addContentToMessage(String htmlContent, Message msg) throws MessagingException {
        MimeMultipart attachments = new MimeMultipart();
        MimeBodyPart textbody = new MimeBodyPart();
        String contentType = getContentType();
        textbody.setContent(htmlContent, contentType);
        attachments.addBodyPart(textbody);

        msg.setContent(attachments);
    }

    String getContentType() {
        if (charset != null) {
            return messageMimeType + "; charset=\"" + charset + "\"";
        } else {
            return messageMimeType;
        }
    }


    /**
     * updates xslFileNames, based on value of xslFileList
     * If first character is +  the list is appended, otherwise
     * the list is replaced. xslFileNames is comma or space-separated
     * list of existing files, located in xslDir. These files are used,
     * in-order, to generate HTML email. If xslFileNames is not
     * specified, xslFileList remains as default.
     * if xslFile is set, this is ignored.
     */
    public void setXSLFileList(String relativePathToXslFile) {
        if (relativePathToXslFile == null || relativePathToXslFile.equals("")) {
            throw new IllegalArgumentException("xslFileList shouldn't be null or empty");
        }

        relativePathToXslFile = relativePathToXslFile.trim();
        boolean appending = relativePathToXslFile.startsWith("+");
        
        if (appending) {
            relativePathToXslFile = relativePathToXslFile.substring(1);
        }

        StringTokenizer st = new StringTokenizer(relativePathToXslFile, " ,");
        int numTokens = st.countTokens();

        int i;
        if (appending) {
            i = xslFileNames.length;
        } else {
            i = 0;
        }
        String[] newXSLFileNames = new String[i + numTokens];
        System.arraycopy(xslFileNames, 0, newXSLFileNames, 0, i);
        
        while (st.hasMoreTokens()) {
            newXSLFileNames[i++] = st.nextToken();
        }
        
        setXSLFileNames(newXSLFileNames);
    }

    /**
     * If xslFile is set then both xslDir and css are ignored. Specified xslFile
     * must take care of entire document -- html open/close, body tags, styles,
     * etc.
     */
    public void setXSLFile(String fullPathToXslFile) {
        xslFile = fullPathToXslFile;
    }

    /**
     * Directory where xsl files are located.
     */
    public void setXSLDir(String xslDirectory) {
        xslDir = xslDirectory;
    }

    /**
     * Method to override the default list of file names that will be looked
     * for in the directory specified by xslDir. By default these are the
     * standard CruseControl xsl files: <br>
     * <ul>
     *   header.xsl
     *   maven.xsl
     *   etc ...
     * </ul>
     * I expect this to be used by a derived class to allow someone to
     * change the order of xsl files or to add/remove one to/from the list
     * or a combination.
     * @param fileNames
     */
    protected void setXSLFileNames(String[] fileNames) {
        if (fileNames == null) {
            throw new IllegalArgumentException("xslFileNames can't be null (but can be empty)");
        }
        xslFileNames = fileNames;
    }

    /**
     * Provided as an alternative to setXSLFileNames for changing the list of 
     * files to use.
     * @return xsl files to use in generating the email
     */
    protected String[] getXslFileNames() {
        return xslFileNames;
    }

    /**
     * Path to cruisecontrol.css.  Only used with xslDir, not xslFile.
     */
    public void setCSS(String cssFilename) {
        css = cssFilename;
    }

    /**
     * Path to the log file as set in the log element of the configuration
     * xml file.
     */
    public void setLogDir(String directory) {
        if (directory == null) {
            throw new IllegalArgumentException("logDir cannot be null!");
        }

        logDir = directory;
    }

    public void setCharset(String characterSet) {
        charset = characterSet;
    }

    /**
     * Create the message to be mailed
     *
     * @param logHelper utility object that has parsed the log files
     * @return created message; empty string if logDir not set
     */

    // TODO: address whether this should ever return null;
    // dependent also on transform(File) and createLinkLine()
    protected String createMessage(XMLLogHelper logHelper) {
        String message = "";

        File inFile = null;
        try {
            if (logDir == null) {
                // use the same default as ProjectXMLHelper.getLog()
                logDir = "logs" + File.separator + logHelper.getProjectName();
            }
            inFile = new File(logDir, logHelper.getLogFileName());
            message = transform(inFile);
        } catch (Exception ex) {
            LOG.error("error transforming " + (inFile == null ? null : inFile.getAbsolutePath()), ex);
            try {
                String logFileName = logHelper.getLogFileName();
                message = createLinkLine(logFileName);
            } catch (CruiseControlException ccx) {
                LOG.error("exception getting logfile name", ccx);
            }
        }

        return message;
    }

    protected String transform(File inFile) throws TransformerException, FileNotFoundException, IOException {
        StringBuffer messageBuffer = new StringBuffer();

        TransformerFactory tFactory = TransformerFactory.newInstance();

        if (xslFile != null) {
            File xslFileAsFile = new File(xslFile);
            appendTransform(inFile, messageBuffer, tFactory, xslFileAsFile);
        } else {
            appendHeader(messageBuffer);
            messageBuffer.append(createLinkLine(inFile.getName()));

            File xslDirectory = new File(xslDir);
            String[] fileNames = getXslFileNames();
            for (int i = 0; i < fileNames.length; i++) {
                String fileName = fileNames[i];
                File xsl = new File(xslDirectory, fileName);
                messageBuffer.append("<p>\n"); 
                appendTransform(inFile, messageBuffer, tFactory, xsl);
            }

            appendFooter(messageBuffer);
        }

        return messageBuffer.toString();
    }

    protected String createLinkLine(String logFileName) {
        StringBuffer linkLine = new StringBuffer("");
        String buildResultsURL = getBuildResultsURL();

        if (buildResultsURL == null) {
            return "";
        }

        int startName = logFileName.lastIndexOf(File.separator) + 1;
        int endName = logFileName.lastIndexOf(".");
        String baseLogFileName = logFileName.substring(startName, endName);
        StringBuffer url = new StringBuffer(buildResultsURL);
        if (buildResultsURL.indexOf("?") == -1) {
            url.append("?");
        } else {
            url.append("&");
        }
        url.append("log=");
        url.append(baseLogFileName);

        linkLine.append("View results here -> <a href=\"");
        linkLine.append(url);
        linkLine.append("\">");
        linkLine.append(url);
        linkLine.append("</a>");

        return linkLine.toString();
    }

    protected void appendTransform(File inFile, StringBuffer messageBuffer, TransformerFactory tFactory, File xsl) {
        try {
            String result = transformFile(new StreamSource(inFile), tFactory, new StreamSource(xsl));
            messageBuffer.append(result);
        } catch (Exception e) {
            LOG.error("error transforming with xslFile " + xsl.getName(), e);
        }
    }
    protected String transformFile(Source logFile, TransformerFactory tFactory, Source xsl)
        throws IOException, TransformerException {
        Transformer transformer = tFactory.newTransformer(xsl);
        CharArrayWriter writer = new CharArrayWriter();
        if (!xsltParameters.isEmpty()) {
            Iterator i = xsltParameters.iterator();
            while (i.hasNext()) {
                Property param = (Property) i.next();
                transformer.setParameter(param.getName(), param.getValue());
            }
        }
        transformer.transform(logFile, new StreamResult(writer));
        return writer.toString();
    }

    protected void appendHeader(StringBuffer messageBuffer) throws IOException {
        messageBuffer.append("<html><head>\n");
        String baseUrl = getBuildResultsURL();
        if (baseUrl != null) {
            messageBuffer.append("<base href=\"").append(baseUrl).append("\">\n");
        }
        messageBuffer.append("<style>\n");

        Util.appendFileToBuffer(css, messageBuffer);

        messageBuffer.append("\n</style>\n</head><body>\n");
    }

    protected void appendFooter(StringBuffer messageBuffer) {
        messageBuffer.append("\n</body></html>");
    }
}
