/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
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
import net.sourceforge.cruisecontrol.CruiseControlOptions;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Title;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.apache.tools.ant.launch.Locator;

/**
 * Used to publish an HTML e-mail that includes the build report
 *
 * @author Jeffrey Fredrick
 * @author Alden Almagro
 * @author <a href="vwiewior@valuecommerce.ne.jp">Victor Wiewiorowski</a>
 */
@Description(
    "<p>Sends an email with the build results embedded as HTML. By default the same information as "
    + "the JSP build results page is sent.</p><p>Typical usage is to define xsldir and css to point "
    + "to cruisecontrol locations. This publisher creates HTML email by transforming information "
    + "based on a set of internally pre-defined xsl files. (Currently \"header.xsl\", and "
    + "\"buildresults.xsl\") This list can be changed, or appended to, using xslfilelist attribute. "
    + "Alternatively, you can specify a single xsl file to handle the full transformation using the "
    + "xslfile attribute.</p>"
)
public class HTMLEmailPublisher extends EmailPublisher {

    private static final long serialVersionUID = 7140930723694451861L;

    private static final Logger LOG = Logger.getLogger(HTMLEmailPublisher.class);

    private String xslFile;
    private String xslDir;
    private String css;
    private String logDir;
    private final String messageMimeType = "text/html";
    private String charset;

    // Should reflect the same stylesheets as buildresults.jsp in the JSP
    // reporting application
    private String[] xslFileNames =
        {
            "header.xsl",
            "buildresults.xsl"
        };

    private final List<Property> xsltParameters = new LinkedList<Property>();

    /*
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    @Override
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

            final String[] fileNames = getXslFileNames();

            if (fileNames == null) {
                throw new CruiseControlException("HTMLEmailPublisher.getXslFileNames() can't return null");
            }

            for (final String fileName : fileNames) {
                verifyFile(
                        "HTMLEmailPublisher.xslDir/" + fileName,
                        new File(xslDir, fileName));
            }
        } else {
            verifyFile("HTMLEmailPublisher.xslFile", xslFile);
        }
    }

    /**
     * @return new parameter that has been added to xslt params list already.
     */
    @Title("Parameter")
    @Description(
            "Parameters passed to the XSL files before transforming them to HTML. Check the "
            + "Reporting application's <a href=\"http://cruisecontrol.sourceforge.net/reporting/"
            + "jsp/custom.html#XSLT_parameters\">documentation</a> for parameters used in the "
            + "standard XSL files."
    )
    @ManualChildName("parameter")
    public Property createParameter() {
        final Property param = new Property();
        xsltParameters.add(param);
        return param;
    }

    /**
     * @return the absolute path where the cruisecontrol.css file is located,
     * or null if it can't be found.
     */
    private String getCssFromClasspath() {
        final File cssFile = guessFileForResource("css/cruisecontrol.css");
        if (cssFile != null && cssFile.exists()) {
            return cssFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * @return the absolute path where the xsl dir is located,
     * or null if it can't be found.
     */
    private String getXslDirFromClasspath() {
        final File xsl = guessFileForResource("xsl");
        if (xsl != null && xsl.isDirectory()) {
            return xsl.getAbsolutePath();
        }
        return null;
    }

    /**
     * Try some path constellations to see if the relative resource exists somewhere.
     * First existing resource will be returned. At the moment we use the CruiseControlOptions.KEY_DIST_DIR
     * config option (preferred) and source-path in combination with the binary-contribution (preferred) and
     * source-tree.
     * @param relativeResource relative path to look for
     * @return an existing resource as file or null
     */
    private File guessFileForResource(final String relativeResource) {
        File ccDist;
        try {
            ccDist = CruiseControlOptions.getInstance().getOptionDir(CruiseControlOptions.KEY_DIST_DIR);
        } catch (CruiseControlException e) {
            LOG.error("Failed to get CC dist directory from config", e);
            ccDist = getCruiseRootDir();
        }

        final String cruise = "reporting/jsp/webcontent/";
        final String binaryDistribution = "webapps/cruisecontrol/";
        final File[] possiblePaths = {new File(ccDist, binaryDistribution + relativeResource),
                new File(ccDist, cruise + relativeResource),
                new File(getCruiseRootDir(), cruise + relativeResource),
                new File(getCruiseRootDir(), binaryDistribution + relativeResource)};
        for (final File possiblePath : possiblePaths) {
            if (possiblePath.exists()) {
                return possiblePath;
            }

        }
        return null;
    }

    /**
     * @return the root directory of the running cruisecontrol installation.
     * Uses Ant's Locator.
     */
    private File getCruiseRootDir() {
        final File classDir = Locator.getClassSource(getClass());
        if (classDir != null) {
            try {
                // we're probably in main/dist/cruisecontrol.jar, so three parents up
                final File rootDir = classDir.getParentFile().getParentFile().getParentFile();
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

    private void verifyDirectory(final String dirName, final String dir) throws CruiseControlException {
        ValidationHelper.assertFalse(dir == null, dirName + " not specified in configuration file");
        final File dirFile = new File(dir);
        ValidationHelper.assertTrue(dirFile.exists(), dirFile + " does not exist: " + dirFile.getAbsolutePath());
        ValidationHelper.assertTrue(dirFile.isDirectory(),
                dirFile + " is not a directory: " + dirFile.getAbsolutePath());
    }

    private void verifyFile(final String fileName, final String file) throws CruiseControlException {
        ValidationHelper.assertFalse(file == null, fileName + " not specified in configuration file");
        verifyFile(fileName, new File(file));
    }

    private void verifyFile(final String fileName, final File file) throws CruiseControlException {
        ValidationHelper.assertTrue(file.exists(), fileName + " does not exist: " + file.getAbsolutePath());
        ValidationHelper.assertTrue(file.isFile(), fileName + " is not a file: " + file.getAbsolutePath());
    }

    /**
     * sets the content as an attachment w/proper mime-type
     */
    @Override
    protected void addContentToMessage(final String htmlContent, final Message msg) throws MessagingException {
        final MimeMultipart attachments = new MimeMultipart();
        final MimeBodyPart textbody = new MimeBodyPart();
        final String contentType = getContentType();
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
     * @param relativePathToXslFile relative path to xsl file
     */
    @Title("XSL File List")
    @Description(
            "Works with xsldir and css. String, representing ordered list of xsl files located in "
            + "xsldir, which are used to format HTML email. List is comma or space separated. If first "
            + "character of list is plus sign (\"+\"), the listed file(s) are added to existing set of "
            + "xsl files used by HTMLEmailPublisher. If xslfilelist is not specified, email is published "
            + "using hard-coded list of xsl files."
    )
    @Optional
    public void setXSLFileList(String relativePathToXslFile) {
        if (relativePathToXslFile == null || relativePathToXslFile.equals("")) {
            throw new IllegalArgumentException("xslFileList shouldn't be null or empty");
        }

        relativePathToXslFile = relativePathToXslFile.trim();
        final boolean appending = relativePathToXslFile.startsWith("+");

        if (appending) {
            relativePathToXslFile = relativePathToXslFile.substring(1);
        }

        final StringTokenizer st = new StringTokenizer(relativePathToXslFile, " ,");
        final int numTokens = st.countTokens();

        int i;
        if (appending) {
            i = xslFileNames.length;
        } else {
            i = 0;
        }
        final String[] newXSLFileNames = new String[i + numTokens];
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
     * @param fullPathToXslFile full path to xsl file
     */
    @Title("XSL File")
    @Description(
            "If specified, xsldir, xslfilelist, and css are ignored. Must handle the "
            + "entire document."
    )
    @Optional
    public void setXSLFile(final String fullPathToXslFile) {
        xslFile = fullPathToXslFile;
    }

    /**
     * Directory where xsl files are located.
     * @param xslDirectory directory where xsl files are located.
     */
    @Title("XSL Dir")
    @Description(
            "Directory where standard CruiseControl xsl files are located. Starting with version "
            + "2.3, the HTMLEmailPublisher will try to determine the correct value itself when it's "
            + "not specified and xslfile isn't used."
    )
    @Optional("<i>Versions up to 2.3</i>: <b>Required</b> unless xslfile specified.")
    public void setXSLDir(final String xslDirectory) {
        xslDir = xslDirectory;
    }

    /**
     * Method to override the default list of file names that will be looked
     * for in the directory specified by xslDir. By default these are the
     * standard CruseControl xsl files: <br>
     * <ul>
     * <li> header.xsl
     * <li> maven.xsl
     * <li> etc ...
     * </ul>
     * I expect this to be used by a derived class to allow someone to
     * change the order of xsl files or to add/remove one to/from the list
     * or a combination.
     * @param fileNames xsl file names to look for
     */
    protected void setXSLFileNames(final String[] fileNames) {
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
     * @param cssFilename css file name
     */
    @Title("CSS")
    @Description(
            "Path to cruisecontrol.css. Used only if xsldir set and not xslfile. Starting with "
            + "version 2.3, the HTMLEmailPublisher will try to determine the correct value itself "
            + "when it's not specified and xslfile isn't used."
    )
    @Optional("<i>Versions up to 2.3</i>: <b>Required</b> unless xslfile specified.")
    public void setCSS(final String cssFilename) {
        css = cssFilename;
    }

    /**
     * Path to the log file as set in the log element of the configuration
     * xml file.
     * @param directory log dir
     */
    @Title("Log Dir")
    @Description(
            "Path to the log directory as set in the log element of the configuration xml file. "
            + "Follows default of <a href=\"#log\">log</a>'s dir-attribute since version 2.2"
    )
    @Optional("Required for versions &lt; 2.2")
    public void setLogDir(final String directory) {
        if (directory == null) {
            throw new IllegalArgumentException("logDir cannot be null!");
        }

        logDir = directory;
    }

    @Title("Charset")
    @Description(
            "If not set the content type will be set to 'text/html'. If set the "
            + "content type will be 'text/html;charset=\"value\"'."
    )
    @Optional
    public void setCharset(final String characterSet) {
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
    @Override
    protected String createMessage(final XMLLogHelper logHelper) {
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
                final String logFileName = logHelper.getLogFileName();
                message = createLinkLine(logFileName);
            } catch (CruiseControlException ccx) {
                LOG.error("exception getting logfile name", ccx);
            }
        }

        return message;
    }

    protected String transform(final File inFile) throws TransformerException, IOException {
        final StringBuilder messageBuffer = new StringBuilder();

        final TransformerFactory tFactory = TransformerFactory.newInstance();

        if (xslFile != null) {
            final File xslFileAsFile = new File(xslFile);
            appendTransform(inFile, messageBuffer, tFactory, xslFileAsFile);
        } else {
            appendHeader(messageBuffer);
            messageBuffer.append(createLinkLine(inFile.getName()));

            final File xslDirectory = new File(xslDir);
            final String[] fileNames = getXslFileNames();
            for (final String fileName : fileNames) {
                final File xsl = new File(xslDirectory, fileName);
                messageBuffer.append("<p>\n");
                appendTransform(inFile, messageBuffer, tFactory, xsl);
            }

            appendFooter(messageBuffer);
        }

        return messageBuffer.toString();
    }

    protected String createLinkLine(final String logFileName) {
        final StringBuilder linkLine = new StringBuilder("");
        final String buildResultsURL = getBuildResultsURL();

        if (buildResultsURL == null) {
            return "";
        }

        final int startName = logFileName.lastIndexOf(File.separator) + 1;
        final int endName = logFileName.lastIndexOf(".");
        final String baseLogFileName = logFileName.substring(startName, endName);
        final StringBuilder url = new StringBuilder(buildResultsURL);
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

    protected void appendTransform(final File inFile, final StringBuilder messageBuffer,
                                   final TransformerFactory tFactory, final File xsl) {
        try {
            final String result = transformFile(new StreamSource(inFile), tFactory, new StreamSource(xsl));
            messageBuffer.append(result);
        } catch (Exception e) {
            LOG.error("error transforming with xslFile " + xsl.getName(), e);
        }
    }
    protected String transformFile(final Source logFile, final TransformerFactory tFactory, final Source xsl)
        throws IOException, TransformerException {

        final Transformer transformer = tFactory.newTransformer(xsl);
        final CharArrayWriter writer = new CharArrayWriter();
        if (!xsltParameters.isEmpty()) {
            for (final Property param : xsltParameters) {
                transformer.setParameter(param.getName(), param.getValue());
            }
        }
        transformer.transform(logFile, new StreamResult(writer));
        return writer.toString();
    }

    protected void appendHeader(final StringBuilder messageBuffer) throws IOException {
        messageBuffer.append("<html><head>\n");
        final String baseUrl = getBuildResultsURL();
        if (baseUrl != null) {
            messageBuffer.append("<base href=\"").append(baseUrl).append("\">\n");
        }
        messageBuffer.append("<style>\n");

        Util.appendFileToBuffer(css, messageBuffer);

        messageBuffer.append("\n</style>\n</head><body>\n");
    }

    protected void appendFooter(final StringBuilder messageBuffer) {
        messageBuffer.append("\n</body></html>");
    }
}
